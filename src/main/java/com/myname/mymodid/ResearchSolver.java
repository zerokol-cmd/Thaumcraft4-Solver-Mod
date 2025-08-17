package com.myname.mymodid;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.utils.HexUtils;

import java.util.*;

/**
 * ResearchSolver with DEEPCOPY removed.
 * Uses shallow snapshots (grid reference shared) to avoid deep copying.
 * <p>
 * NOTE: Because snapshots share the same grid reference, backtracking will not restore any
 * in-place modifications to individual Cell objects. If you require full undo, replace
 * snapshots with deep copies or implement an action/undo log.
 */
public class ResearchSolver {

    public static class Hex { // returned result element (public)

        public byte posX;
        public byte posY;
        public Aspect aspect;

        public Hex(byte x, byte y, Aspect a) {
            posX = x;
            posY = y;
            aspect = a;
        }
    }

    static class Size {

        int x;
        int y;

        public Size(int width, int height) {
            x = width;
            y = height;
        }
    }

    private static class State {
        HashMap<String,Cell> currentPathForbiddenChild;
        Cell[][] grid;
        List<String> currentPath = null; // list of ids
        String badStateReason = null;
        List<List<String>> blacklistedPaths = new ArrayList<>();
        Map<String, Set<Aspect>> impossibleAspectsForCurrentPath = new HashMap<>();
        Map<String, Set<String>> impossibleClosestCells = new HashMap<>();
        String lastCell = null;
        String[] lastMove = null;
    }

    private static class Cell {

        String id; // "x:y" original id
        int arrayX, arrayY; // indices in grid array
        Aspect aspect; // null => 'none'
        boolean barred;
        Set<Aspect> blacklisted = new HashSet<>();

        Cell(String id, int arrayX, int arrayY, Aspect aspect) {
            this.id = id;
            this.arrayX = arrayX;
            this.arrayY = arrayY;
            this.aspect = aspect;
            this.barred = false;
        }

        boolean aspectIsNone() {
            return aspect == null;
        }

        Cell shallowCopy() {
            Cell c = new Cell(id, arrayX, arrayY, aspect);
            c.blacklisted = new HashSet<>(blacklisted);
            return c;
        }
    }

    State currentState;

    ResearchNoteData originalNote;
    ResearchNoteData note;
    String username;

    boolean complete = false;
    int depth = 0;

    // Grid offsets (so keys "x:y" map to array indices)
    private int minX = 0, minY = 0;
    private int width = 0, height = 0;

    public double getDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.abs(x1 - x2) * Math.abs(x1 - x2) +
            Math.abs(y1 - y2) * Math.abs(y1 - y2));
    }

    private Map<String, Map<String, Double>> distanceDict = new HashMap<>();

    private boolean pathComplete(List<String> path, State state) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        // Get first cell in path
        Cell firstPathCell = getCellById(path.get(0), state.grid);
        if (firstPathCell == null) {
            return false;
        }

        // Get neighbor chain
        List<int[]> neighborPositions = neighborChain(firstPathCell.arrayX, firstPathCell.arrayY, state.grid);
        List<String> neighborIds = new ArrayList<>();

        // Convert positions to cell IDs
        for (int[] pos : neighborPositions) {
            Cell neighborCell = state.grid[pos[0]][pos[1]];
            neighborIds.add(neighborCell.id);
        }

        // Check if path is contained in neighbors
        return neighborIds.containsAll(path);
    }

    // Helper method to get cell by ID
    private Cell getCellById(String id, Cell[][] grid) {
        for (Cell[] row : grid) {
            for (Cell cell : row) {
                if (cell.id.equals(id)) {
                    return cell;
                }
            }
        }
        return null;
    }

    private int lastUsedPathCell(State state, List<String> path) {
        if (path == null) {
            path = state.currentPath;
        }
        if (path == null || path.isEmpty()) {
            return -1; // or throw an exception
        }

        // Get first cell in path
        Cell firstCell = getCellById(path.get(0), state.grid);
        if (firstCell == null) {
            return -1;
        }

        // Get neighbor chain
        List<int[]> neighborChain = neighborChain(firstCell.arrayX, firstCell.arrayY, state.grid);
        List<String> neighborIds = new ArrayList<>();
        for (int[] pos : neighborChain) {
            neighborIds.add(state.grid[pos[0]][pos[1]].id);
        }

        // Check each cell in path
        for (int i = 1; i < path.size(); i++) {
            // Check if last cell is not in neighbor chain
            if (i == path.size() - 1 && !neighborIds.contains(path.get(i))) {
                return path.size() - 2;
            }

            // Check if current cell has no aspect
            Cell currentCell = getCellById(path.get(i), state.grid);
            if (currentCell != null && currentCell.aspectIsNone()) {
                return i - 1;
            }
        }

        // If all checks passed, return last index
        return path.size() - 1;
    }

    private boolean inSameSpace(String id1, String id2, Cell[][] grid, boolean aspects, List<String> currentPath) {
        // Get both cells by their IDs
        Cell cell1 = getCellById(id1, grid);
        Cell cell2 = getCellById(id2, grid);

        // Check if either cell is null
        if (cell1 == null || cell2 == null) {
            return false;
        }

        // Get neighbor chain with additional parameters
        List<int[]> tempChain = neighborChain(
            cell1.arrayX,
            cell1.arrayY,
            grid,
            true,  // includeDiagonal
            null,  // excludeId
            aspects,
            id2,
            currentPath
        );

        // Check if cell2's position is in the neighbor chain
        return containsPosition(tempChain, cell2.arrayX, cell2.arrayY);
    }

    // Helper method to check if a position exists in the chain
    private boolean containsPosition(List<int[]> positions, int x, int y) {
        for (int[] pos : positions) {
            if (pos[0] == x && pos[1] == y) {
                return true;
            }
        }
        return false;
    }
    private boolean idInBlacklist(String id, State state) {
        if (state.impossibleClosestCells == null) {
            return false;
        }
        return state.impossibleClosestCells.containsKey(id);
    }

    // Checks if an aspect is blacklisted for a specific ID
    private boolean aspectBlacklistedAtId(Aspect aspect, String id, State state) {
        if (state.impossibleClosestCells == null) {
            return false;
        }
        Set<Aspect> blacklistedAspects = state.impossibleClosestCells.get(id);
        return blacklistedAspects != null && blacklistedAspects.contains(aspect);
    }

    // Determines if a cell is completely unusable (all aspects blacklisted)
    private boolean impossibleId(String id, State state) {
        if (!idInBlacklist(id, state)) {
            return false;
        }

        Set<String> blacklistedAspects = state.impossibleClosestCells.get(id);
        Map<Aspect, ?> aspectTable = getAspectTable(); // Assuming this exists

        return blacklistedAspects != null &&
            aspectTable != null &&
            blacklistedAspects.size() == aspectTable.size();
    }

    // Helper method - assuming you have this defined elsewhere
    private Map<Aspect, ?> getAspectTable() {
        // Implementation depends on your application
        return null;
    }
    String closestNeighborToCell(String id1, String id2, State state, List<String> path,
                                 List<String> customBlacklist, boolean findingPath,
                                 List<String> currentPath) {
        // Handle null cases for optional parameters
        if (path == null) path = new ArrayList<>();
        if (customBlacklist == null) customBlacklist = new ArrayList<>();
        if (currentPath == null) currentPath = new ArrayList<>();

        Cell cell1 = getCellById(id1, state.grid);
        Cell cell2 = getCellById(id2, state.grid);
        if (cell1 == null || cell2 == null) {
            return null;
        }

        List<int[]> tempNeighbors = tileNeighborsWithoutAspects(cell1.arrayX, cell1.arrayY, state.grid);
        String tempClosestNeighbor = null;
        Double tempClosestDistance = null;

        for (int[] neighborPos : tempNeighbors) {
            Cell target = state.grid[neighborPos[0]][neighborPos[1]];

            // Check basic conditions
            boolean basicConditions = target.aspectIsNone() && !target.barred
                && (!findingPath || inSameSpace(cell2.id, target.id, state.grid, true, currentPath))
                && (state.currentPath == null || state.currentPath.isEmpty()
                || target.id.equals(state.currentPath.get(lastUsedPathCell(state, state.currentPath) + 1)));

            if (basicConditions) {
                // Check additional conditions
                boolean additionalConditions = !customBlacklist.contains(target.id)
                    && (path.isEmpty() == path.contains(target.id))
                    && (state.currentPathForbiddenChild == null
                    || !target.id.equals(state.currentPathForbiddenChild.get(id1)))
                    && (!findingPath || !currentPath.contains(target.id));

                if (additionalConditions) {
                    double currentDistance = distanceDict.get(id2).get(target.id);

                    if (tempClosestNeighbor == null || currentDistance < tempClosestDistance) {
                        tempClosestNeighbor = target.id;
                        tempClosestDistance = currentDistance;
                    }
                }
            }
        }

        return tempClosestNeighbor;
    }
    private String[] closestCells(State state) {
        // Experimental part - if there's a current path and it's not complete
//        if (state.currentPath != null && !pathComplete(state.currentPath, state)) {
//            int lastUsed = lastUsedPathCell(state, null);
//            String lastInPath = state.currentPath.get(state.currentPath.size() - 1);
//            return new String[]{state.currentPath.get(lastUsed), lastInPath};
//        }

        String[] currentClosestPair = new String[0];
        double currentMinDistance = Double.MAX_VALUE;

        for (int i = 0; i < state.grid.length; i++) {
            for (int j = 0; j < state.grid[i].length; j++) {
                Cell cell = state.grid[i][j];
                // Check that the cell has an aspect and not in blacklist
                if (!cell.aspectIsNone() && !impossibleId(cell.id, state)) {
                    List<int[]> currentNeighborChain = neighborChain(i, j, state.grid);

                    for (int k = 0; k < state.grid.length; k++) {
                        for (int l = 0; l < state.grid[k].length; l++) {
                            Cell otherCell = state.grid[k][l];
                            // Skip if same cell
                            if (i == k && j == l) continue;

                            // Check if the cell isn't already connected
                            boolean inNeighborChain = false;
                            for (int[] pos : currentNeighborChain) {
                                if (pos[0] == k && pos[1] == l) {
                                    inNeighborChain = true;
                                    break;
                                }
                            }

                            // Additional checks from original code
                            if (state.currentPath != null && !pathComplete(currentState.currentPath, currentState)
                                && Objects.equals(closestNeighborToCell(cell.id, otherCell.id, state, null, null, false, null), "")) {
                                continue;
                            }

                            if (state.currentPath != null && !pathComplete(currentState.currentPath, currentState) {
                                if (!cell.id.equals(state.currentPath.get(lastUsedPathCell(state, null)))) {
                                    continue;
                                }
                            }

                            if (!inNeighborChain && !otherCell.aspectIsNone() && !impossibleId(otherCell.id, state)
                                && inSameSpace(cell.id, otherCell.id, state.grid, true, currentState.currentPath)) {

                                double potentialDistance = getDistance(cell.arrayX, cell.arrayY, otherCell.arrayX, otherCell.arrayY);

                                if (currentClosestPair.length == 0 || potentialDistance < currentMinDistance) {
                                    currentClosestPair = new String[]{cell.id, otherCell.id};
                                    currentMinDistance = potentialDistance;

                                    if (state.currentPath != null) {
                                        currentClosestPair = sortTuple(currentClosestPair, state.currentPath);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (currentClosestPair.length == 0 && state.currentPath != null) {
            return new String[]{
                state.currentPath.get(0),
                state.currentPath.get(state.currentPath.size() - 1)
            };
        }
        return currentClosestPair;
    }

    private void precalculateCellDistances() {
        System.out.println("precalculating distances: start");
        Cell[][] grid = currentState.grid;

        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                String id1 = grid[i][j].id;
                distanceDict.putIfAbsent(id1, new HashMap<>());

                for (int k = 0; k < grid.length; k++) {
                    for (int l = 0; l < grid[k].length; l++) {
                        if (!(i == k && j == l)) {
                            String id2 = grid[k][l].id;
                            double distance = getDistance(grid[i][j].arrayX, grid[i][j].arrayY,
                                grid[k][l].arrayX, grid[k][l].arrayY);
                            distanceDict.get(id1).put(id2, distance);
                        }
                    }
                }
            }
        }
        System.out.println("precalculating distances: done");
    }

    private void buildGrid() {
        // Get bounds (same as before)
        int minx = Integer.MAX_VALUE, miny = Integer.MAX_VALUE, maxx = Integer.MIN_VALUE, maxy = Integer.MIN_VALUE;
        for (String key : note.hexEntries.keySet()) {
            String[] parts = key.split(":");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            if (x < minx) minx = x;
            if (y < miny) miny = y;
            if (x > maxx) maxx = x;
            if (y > maxy) maxy = y;
        }
        minX = minx;
        minY = miny;
        width = (maxx - minx) + 1;
        height = (maxy - miny) + 1;

        Cell[][] denseGrid = new Cell[width][height];
        for (int gx = 0; gx < width; gx++) {
            for (int gy = 0; gy < height; gy++) {
                String id = (gx + minX) + ":" + (gy + minY);

                ResearchManager.HexEntry he = note.hexEntries.get(id);
                Aspect a = (he != null) ? he.aspect : null; // Use `null` if no entry

                denseGrid[gx][gy] = new Cell(id, gx, gy, a);
            }
        }

        // Store the generated grid
        currentState = new State();
        currentState.grid = denseGrid;
    }

    public ResearchSolver(ResearchNoteData note, String username) {
        this.originalNote = note;
        this.note = note; // using same note reference; clone if you want independent mutability
        this.username = username;
        buildGrid();
        for (int x = 0; x < currentState.grid.length; x++) {
            for (int y = 0; y < currentState.grid[x].length; y++) {
                System.out.println("Cell [" + x + "][" + y + "] = " + currentState.grid[x][y].aspectIsNone());
            }
        }

    }

    boolean returning_from_bad_state = false;

    private void mainLoopIteration() {
        System.out.println("entering depth " + depth);
        depth++;

        if (isComplete(note, username)) {
            complete = true;
            System.out.println("==============\nS O L V E D\n==============");
            return;
        }
        if (!returning_from_bad_state) {
            var temp_closest_cells = closest_cells(current_state);

        }
    }

    public Hex[] Solve() {
        depth++;

        while (!complete) {
            mainLoopIteration();
            if (depth > 10) {
                throw new RuntimeException("ResearchSolver depth overflow");
            }
        }
        return new Hex[1];
    }

    static void checkConnections(ResearchNoteData note, HexUtils.Hex hex, ArrayList<String> checked,
                                 ArrayList<String> main, ArrayList<String> remains, String username) {
        checked.add(hex.toString());
        for (int a = 0; a < 6; ++a) {
            HexUtils.Hex target = hex.getNeighbour(a);
            if (checked.contains(target.toString()) || !note.hexEntries.containsKey(target.toString())
                || note.hexEntries.get((Object) target.toString()).type < 1) continue;
            Aspect aspect1 = note.hexEntries.get((Object) hex.toString()).aspect;
            Aspect aspect2 = note.hexEntries.get((Object) target.toString()).aspect;
            if (!Thaumcraft.proxy.getPlayerKnowledge()
                .hasDiscoveredAspect(username, aspect1)
                || !Thaumcraft.proxy.getPlayerKnowledge()
                .hasDiscoveredAspect(username, aspect2)
                || (aspect1.isPrimal()
                || aspect1.getComponents()[0] != aspect2 && aspect1.getComponents()[1] != aspect2)
                && (aspect2.isPrimal()
                || aspect2.getComponents()[0] != aspect1 && aspect2.getComponents()[1] != aspect1))
                continue;
            remains.add(target.toString());
            if (note.hexEntries.get((Object) target.toString()).type == 1) {
                main.remove(target.toString());
            }
            checkConnections(note, target, checked, main, remains, username);
        }
    }

    public static boolean isComplete(ResearchNoteData note, String username) {
        ArrayList<String> checked = new ArrayList<>();
        ArrayList<String> main = new ArrayList<>();
        ArrayList<String> remains = new ArrayList<>();
        for (Object val : note.hexes.values()) {
            HexUtils.Hex hex = (HexUtils.Hex) val;
            if (note.hexEntries.get((Object) hex.toString()).type != 1) continue;
            main.add(hex.toString());
        }
        for (Object val : note.hexes.values()) {
            HexUtils.Hex hex = (HexUtils.Hex) val;
            if (note.hexEntries.get((Object) hex.toString()).type != 1) continue;
            main.remove(hex.toString());
            checkConnections(note, hex, checked, main, remains, username);
            break;
        }
        if (main.isEmpty()) {
            ArrayList<String> remove = new ArrayList<>();
            for (Object val : note.hexes.values()) {
                HexUtils.Hex hex = (HexUtils.Hex) val;
                if (note.hexEntries.get((Object) hex.toString()).type == 1 || remains.contains(hex.toString()))
                    continue;
                remove.add(hex.toString());
            }
            for (String s : remove) {
                note.hexEntries.remove(s);
                note.hexes.remove(s);
            }
            return true;
        }
        return false;
    }
}

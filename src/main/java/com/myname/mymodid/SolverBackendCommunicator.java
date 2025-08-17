package com.myname.mymodid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.research.ResearchManager;

public class SolverBackendCommunicator {

    SolverBackendCommunicator() {

    }

    private static String escape(String s) {
        return s.replace("\"", "\\\"");
    }

    public static String pack(Map<String, ResearchManager.HexEntry> hexEntries,
        Map<String, AspectList> aspectsDiscovered) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // hexEntries
        sb.append("\"hexEntries\":{");
        boolean first = true;
        for (Map.Entry<String, ResearchManager.HexEntry> entry : hexEntries.entrySet()) {
            if (!first) sb.append(",");
            first = false;

            ResearchManager.HexEntry hex = entry.getValue();
            String aspectName = (hex.aspect != null) ? hex.aspect.getName() : "none";

            sb.append("\"")
                .append(escape(entry.getKey()))
                .append("\":{");
            sb.append("\"aspect\":\"")
                .append(escape(aspectName))
                .append("\",");
            sb.append("\"type\":")
                .append(hex.type);
            sb.append("}");
        }
        sb.append("},");

        // aspectsDiscovered
        sb.append("\"aspectsDiscovered\":{");
        first = true;
        for (Map.Entry<String, AspectList> entry : aspectsDiscovered.entrySet()) {
            if (!first) sb.append(",");
            first = false;

            sb.append("\"")
                .append(escape(entry.getKey()))
                .append("\":{");

            if (entry.getValue() == null || entry.getValue().aspects.isEmpty()) {
                sb.append("\"none\":0");
            } else {
                boolean firstAsp = true;
                for (Map.Entry<Aspect, Integer> asp : entry.getValue().aspects.entrySet()) {
                    if (!firstAsp) sb.append(",");
                    firstAsp = false;

                    String aspectName = (asp.getKey() != null) ? asp.getKey()
                        .getName() : "none";
                    sb.append("\"")
                        .append(escape(aspectName))
                        .append("\":")
                        .append(asp.getValue());
                }
            }

            sb.append("}");
        }
        sb.append("}");

        sb.append("}");
        return sb.toString();
    }

    public class Cell {

        int x, y;
        int array_x, array_y;
        String aspect;

        Cell(int x, int y, int array_x, int array_y, String aspect) {
            this.array_x = array_x;
            this.array_y = array_y;
            this.x = x;
            this.y = y;
            this.aspect = aspect;
        }

        public static void recenter(ArrayList<Cell> cells) {
            if (cells == null || cells.isEmpty()) return;

            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;

            // Find current min/max coordinates
            for (Cell cell : cells) {
                if (cell.x < minX) minX = cell.x;
                if (cell.x > maxX) maxX = cell.x;
                if (cell.y < minY) minY = cell.y;
                if (cell.y > maxY) maxY = cell.y;
            }

            // Compute offsets to center
            double offsetX = (maxX + minX) / 2.0;
            double offsetY = (maxY + minY) / 2.0;

            // Apply offset to all cells
            for (Cell cell : cells) {
                cell.x = (int) Math.round(cell.x - offsetX);
                cell.y = (int) Math.round(cell.y - offsetY);
            }
        }
    };

    public ArrayList<Cell> RequestSolution(HashMap<String, ResearchManager.HexEntry> hexEntries,
        Map<String, AspectList> aspectsDiscovered) throws IOException {
        // Pack into JSON
        ArrayList<Cell> cells = new ArrayList<>();
        String json = pack(hexEntries, aspectsDiscovered);
        System.out.println("Sending:\n" + json);

        // Create URL connection
        URL url = new URL("http://localhost:3000");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        // Send JSON body
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = json.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Read response
        int code = conn.getResponseCode();
        System.out.println("Response code: " + code);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.toString());
            // root is a 2D array

            for (JsonNode row : root) {
                for (JsonNode cellNode : row) {
                    int x = cellNode.get("x")
                        .asInt();
                    int y = cellNode.get("y")
                        .asInt();
                    int arrayX = cellNode.get("array_x")
                        .asInt();
                    int arrayY = cellNode.get("array_y")
                        .asInt();
                    String aspect = cellNode.get("aspect")
                        .asText();

                    Cell cell = new Cell(x, y, arrayX, arrayY, aspect);
                    cells.add(cell);
                }
            }

        }

        conn.disconnect();
        return cells;
    }
}

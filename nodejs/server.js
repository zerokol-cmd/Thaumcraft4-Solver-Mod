var debugging = false;
var preset_num = 6;
var grid = [];
var holding_aspect = false;
var held_aspect = 'none';
var last_highlighted_id = null;
var body_color = '#1C1D1F';

/* detect click outside of canvas */
var image_cache = {};

/* util functions, going into separate file later */


/* degrees to radians */
function to_rad(deg) {
	return deg * Math.PI / 180;
}

/* distance between two points */
function get_distance(x1, y1, x2, y2) {
	return Math.sqrt(Math.abs(x1 - x2) * Math.abs(x1 - x2) + Math.abs(y1 - y2) * Math.abs(y1 - y2));
}

/* object handling */
function keylen(obj) {
	return Object.keys(obj).length;
}

function keyAtIndex(obj, index) {
	return Object.keys(obj)[index];
}

function elemAtIndex(obj, index) {
	return obj[Object.keys(obj)[index]];
}

function indexOfElem(obj, elem) {
	for (let i = 0; i < keylen(obj); i++) {
		if (keyAtIndex(obj, i) == elem) {
			return i;
		}
	}
}

function obj_expr_xnor(object, expression) {
	if (object === 'undefined') {
		return true;
	} else {
		return expression;
	}
}

function capitalize(string) {
	return string[0].toUpperCase() + string.substring(1);
}

function communize(string) {
	return string[0].toLowerCase() + string.substring(1);
}


var aspect_flavor_dict = {
	'aer': 'air',
	'alienis': 'eldritch',
	'aqua': 'water',
	'arbor': 'tree',
	'auram': 'aura',
	'bestia': 'beast',
	'cognitio': 'mind',
	'corpus': 'flesh',
	'exanimis': 'undead',
	'fabrico': 'craft',
	'fames': 'hunger',
	'gelum': 'cold',
	'herba': 'plant',
	'humanus': 'man',
	'ignis': 'fire',
	'instrumentum': 'tool',
	'iter': 'travel',
	'limus': 'slime',
	'lucrum': 'desire',
	'lux': 'light',
	'machina': 'mechanism',
	'messis': 'crop',
	'metallum': 'metal',
	'meto': 'harvest',
	'mortuus': 'death',
	'motus': 'motion',
	'ordo': 'order',
	'pannus': 'cloth',
	'perditio': 'entropy',
	'perfodio': 'mine',
	'permutatio': 'exchange',
	'potentia': 'energy',
	'praecantatio': 'magic',
	'sano': 'health',
	'sensus': 'senses',
	'spiritus': 'soul',
	'telum': 'weapon',
	'tempestas': 'weather',
	'tenebrae': 'darkness',
	'terra': 'earth',
	'tutamen': 'armor',
	'vacuos': 'void',
	'venenum': 'poison',
	'victus': 'life',
	'vinculum': 'trap',
	'vitium': 'taint',
	'vitreus': 'crystal',
	'volatus': 'flight',
	'desidia': 'sloth',
	'gula': 'gluttony',
	'infernus': 'nether',
	'invidia': 'envy',
	'ira': 'wrath',
	'luxuria': 'lust',
	'superbia': 'pride',
	'tempus': 'time',

	'electrum': 'electricity',
	'magneto': 'magnetism',
	'nebrisum': 'cheatiness',
	'radio': 'radioactivity',
	'strontio': 'stupidity',
	'terminus': 'apocalypse',
	'coralos': 'coralium',
	'dreadia': 'dread',
	'tincturem': 'color',
	'sanctus': 'holiness',
	'exubitor': 'warden',
	'saxum': 'stone',
	'granum': 'seed',
	'mru': 'magical radiation unit',
	'radiation': 'radiation',
	'matrix': 'protection'
}

var all_aspect_flavor_dict = {};
for (let i = 0; i < keylen(aspect_flavor_dict); i++) {
	all_aspect_flavor_dict[keyAtIndex(aspect_flavor_dict, i)] = elemAtIndex(aspect_flavor_dict, i);
}

var enabled_flavor_dict = {};
for (let i = 0; i < keylen(aspect_flavor_dict); i++) {
	enabled_flavor_dict[keyAtIndex(aspect_flavor_dict, i)] = true;
}




class Cell {
	constructor(x, y, aspect, barred, side_len, id, array_x, array_y) {
		this.x = x;
		this.y = y;
		this.aspect = aspect;
		this.barred = barred;
		this.side_len = side_len;
		this.selected = false;
		this.highlighted = false;
		this.img;
		this.base = false;
		this.id = id;
		this.parent_id;
		this.array_x = array_x;
		this.array_y = array_y;
		this.path_child;
		//this.path_parent;
	}
}

var distance_dict = {};

class State {
	constructor(grid) {
		this.grid = grid;
		this.impossible_closest_cells = {};
		this.impossible_aspects_for_current_path = {};
		this.impossible_last_cell_id;
		this.last_move = null;
		this.last_cell = null;
		this.last_cell2 = null;
		this.current_path = null;
		this.blacklisted_paths = []; /* I swear, I'll update my terminology ASAP, straight from the master branch of the political correctness repo */
		this.blacklisted_cells = [];
		this.current_path_child_dict = {};
		this.current_path_child_blacklist = {};
		this.current_path_forbidden_child;
		this.bad_state_reason = null;
	}
}

var mouse_mode = 'selecting';
var selected_cell;

/* drawing default size hex grid. Regular hexagon, "size 4" */
/* the whole grid should always take the maximum space */
/* a place in the grid should be selected on the canvas and then the aspect that is there must be a html panel below where one can search for the aspect and click to place it */
var grid_size = 5;




function remove_from_aspect_table(aspect) {
	let res = {};
	for (let i = 0; i < keylen(aspect_table); i++) {
		if (keyAtIndex(aspect_table, i) != aspect) {
			res[keyAtIndex(aspect_table, i)] = elemAtIndex(aspect_table, i);
		}
	}
	aspect_table = res;
}

function add_to_aspect_table(aspect) {
	let one = null;
	let two = null;
	if (all_aspect_table[aspect].length > 0) {
		one = all_aspect_table[aspect][0];
		two = all_aspect_table[aspect][1];
		aspect_table[aspect] = [one, two];
	} else {
		aspect_table[aspect] = [];
	}
}

var aspect_table = {
	'aer': [],
	'alienis': ['vacuos', 'tenebrae'],
	'aqua': [],
	'arbor': ['aer', 'herba'],
	'auram': ['aer', 'praecantatio'],
	'bestia': ['motus', 'victus'],
	'cognitio': ['ignis', 'spiritus'],
	'corpus': ['bestia', 'mortuus'],
	'exanimis': ['motus', 'mortuus'],
	'fabrico': ['humanus', 'instrumentum'],
	'fames': ['vacuos', 'victus'],
	'gelum': ['ignis', 'perditio'],
	'herba': ['terra', 'victus'],
	'humanus': ['bestia', 'cognitio'],
	'ignis': [],
	'instrumentum': ['ordo', 'humanus'],
	'iter': ['terra', 'motus'],
	'limus': ['aqua', 'victus'],
	'lucrum': ['fames', 'humanus'],
	'lux': ['aer', 'ignis'],
	'machina': ['motus', 'instrumentum'],
	'messis': ['herba', 'humanus'],
	'metallum': ['terra', 'vitreus'],
	'meto': ['instrumentum', 'messis'],
	'mortuus': ['perditio', 'victus'],
	'motus': ['aer', 'ordo'],
	'ordo': [],
	'pannus': ['bestia', 'instrumentum'],
	'perditio': [],
	'perfodio': ['terra', 'humanus'],
	'permutatio': ['ordo', 'perditio'],
	'potentia': ['ignis', 'ordo'],
	'praecantatio': ['potentia', 'vacuos'],
	'sano': ['ordo', 'victus'],
	'sensus': ['aer', 'spiritus'],
	'spiritus': ['victus', 'mortuus'],
	'telum': ['ignis', 'instrumentum'],
	'tempestas': ['aer', 'aqua'],
	'tenebrae': ['lux', 'vacuos'],
	'terra': [],
	'tutamen': ['terra', 'instrumentum'],
	'vacuos': ['aer', 'perditio'],
	'venenum': ['aqua', 'perditio'],
	'victus': ['aqua', 'terra'],
	'vinculum': ['perditio', 'motus'],
	'vitium': ['perditio', 'praecantatio'],
	'vitreus': ['ordo', 'terra'],
	'volatus': ['aer', 'motus'],

	'desidia': ['vinculum', 'spiritus'],
	'gula': ['fames', 'vacuos'],
	'infernus': ['ignis', 'praecantatio'],
	'invidia': ['sensus', 'fames'],
	'ira': ['telum', 'ignis'],
	'luxuria': ['corpus', 'fames'],
	'superbia': ['volatus', 'vacuos'],
	'tempus': ['vacuos', 'ordo'],
	'electrum': ['potentia', 'machina'],
	'magneto': ['metallum', 'iter'],
	'nebrisum': ['perfodio', 'lucrum'],
	'radio': ['lux', 'potentia'],
	'strontio': ['perditio', 'cognitio'],
	//'terminus': ['alienis', 'lucrum'],

	// 'coralos': ['venenum', 'aqua'],
	// 'dreadia': ['venenum', 'ignis'],
	// 'tincturem': ['lux', 'ordo'],
	// 'sanctus': ['spiritus', 'auram'],
	// 'exubitor': ['alienis', 'mortuus'],
	// 'saxum': ['terra', 'terra'],
	// 'granum': ['terra', 'victus'],
	// 'mru': ['praecantatio', 'potentia'],
	// 'radiation': ['mru', 'motus'],
	// 'matrix': ['mru', 'humanus']
};

var all_aspect_table = {};
for (let i = 0; i < keylen(aspect_table); i++) {
	all_aspect_table[keyAtIndex(aspect_table, i)] = elemAtIndex(aspect_table, i);
}


function toggle_aspect_set(checkbox_wrapper) {
	let checkbox = checkbox_wrapper.childNodes[1];
	let aspect_set = {
		vanilla_tc: ['aer', 'alienis', 'aqua', 'arbor', 'auram', 'bestia', 'cognitio', 'corpus', 'exanimis', 'fabrico', 'fames', 'gelum', 'herba', 'humanus', 'ignis', 'instrumentum', 'iter', 'limus', 'lucrum', 'lux', 'machina', 'messis', 'metallum', 'meto', 'mortuus', 'motus', 'ordo', 'pannus', 'perditio', 'perfodio', 'permutatio', 'potentia', 'praecantatio', 'sano', 'sensus', 'spiritus', 'telum', 'tempestas', 'tenebrae', 'terra', 'tutamen', 'vacuos', 'venenum', 'victus', 'vinculum', 'vitium', 'vitreus', 'volatus'],
		forbidden_magic: ['desidia', 'gula', 'infernus', 'invidia', 'ira', 'luxuria', 'superbia'],
		//gregtech: ['electrum', 'magneto', 'nebrisum', 'radio', 'strontio'],
		//magic_bees: ['tempus'],
		//avaritia: ['terminus'],
		//abyssal: ['coralos', 'dreadia'],
		//botanical: ['tincturem'],
		//elysium: ['sanctus'],
		//revelations: ['exubitor'],
		//additions: ['saxum', 'granum'],
		//essential: ['mru', 'radiation', 'matrix']
	};

	if (!checkbox.checked) {
		for (let i = 0; i < aspect_set[checkbox.value].length; i++) {
			for (let j = 0; j < keylen(all_aspect_table); j++) {
				if (keyAtIndex(all_aspect_table, j) == aspect_set[checkbox.value][i]) {
					remove_from_aspect_table(aspect_set[checkbox.value][i]);
					enabled_flavor_dict[keyAtIndex(all_aspect_table, j)] = false;
				}
			}
		}
	} else {
		for (var i = 0; i < aspect_set[checkbox.value].length; i++) {
			add_to_aspect_table(aspect_set[checkbox.value][i]);
			enabled_flavor_dict[keyAtIndex(all_aspect_table, indexOfElem(all_aspect_table, aspect_set[checkbox.value][i]))] = true;
		}
	}
	load_aspect_selector(false);
}


function compounds(aspect) {
	return aspect_table[aspect];
}

/* for comparing tuples */
function inArray(haystack, needle) {
	for (item in haystack) {
		if (haystack[item][0] == needle[0] && haystack[item][1] == needle[1]) {
			return true;
		}
	}
	return false;
}

function in_normal_array(haystack, needle) {
	for (let i = 0; i < haystack.length; i++) {
		if (haystack[i] == needle) {
			return true;
		}
	}
	return false;
}

function array_contains_array(container, contained) {
	for (let i = 0; i < contained.length; i++) {
		let found = false;
		for (let j = 0; j < container.length; j++) {
			if (container[j] == contained[i]) {
				found = true;
			}
		}
		if (!found) {
			return false;
		}
	}
	return true;
}

function deepcopyOLD(object) {
	if (Array.isArray(object)) {
		var copy = [];
	} else if (typeof object === "object") {
		var copy = {};
	} else {
		var copy = [];
	}
	for (index in object) {
		if (Array.isArray(object[index]) || typeof object[index] === "object") {
			copy[index] = deepcopy(object[index]);
		} else {
			copy[index] = object[index];
		}
	}
	return copy;
}

/* source: http://voidcanvas.com/clone-an-object-in-vanilla-js-in-depth/ */
function deepcopy(obj) {
	if (obj === null || typeof obj !== "object") {
		return obj;
	} else if (Array.isArray(obj)) {
		var clonedArr = [];
		obj.forEach(function (element) {
			clonedArr.push(deepcopy(element))
		});
		return clonedArr;
	} else {
		let clonedObj = {};
		for (let prop in obj) {
			if (obj.hasOwnProperty(prop)) {
				clonedObj[prop] = deepcopy(obj[prop]);
			}
		}
		return clonedObj;
	}
}

function can_connect(aspect1, aspect2) {
	if (all_aspect_table[aspect1].includes(aspect2) || all_aspect_table[aspect2].includes(aspect1)) {
		return true;
	}
	return false;
}

function cell_exists(grid, x, y) {
	if (typeof grid[x] === 'undefined') {
		return false;
	}
	if (typeof grid[x][y] === 'undefined') {
		return false;
	}
	return true;
}

function paint_tiles(coord_list) {
	var real_coords = [];
	for (let i = 0; i < coord_list.length; i++) {
		real_coords.push([grid[coord_list[i][0]][coord_list[i][1]].x, grid[coord_list[i][0]][coord_list[i][1]].y]);
	}
	for (let i = 0; i < real_coords.length; i++) {
		draw_hexagon(real_coords[i][0], real_coords[i][1], grid[0][0].side_len, undefined, undefined, '#000000');
	}
}

function tile_neighbors(x, y, grid, all = false) {
	var left_coords = [[x, y - 1], [x + 1, y], [x + 1, y + 1], [x, y + 1], [x - 1, y], [x - 1, y - 1]];
	var middle_coords = [[x, y - 1], [x + 1, y - 1], [x + 1, y], [x, y + 1], [x - 1, y], [x - 1, y - 1]];
	var right_coords = [[x, y - 1], [x + 1, y - 1], [x + 1, y], [x, y + 1], [x - 1, y + 1], [x - 1, y]];
	if (x == Math.floor(grid.length / 2)) {
		for (let i = 0; i < middle_coords.length; i++) {
			if (!cell_exists(grid, middle_coords[i][0], middle_coords[i][1]) || grid[middle_coords[i][0]][middle_coords[i][1]].aspect == 'none' || !((all_aspect_table[grid[x][y].aspect].includes(grid[middle_coords[i][0]][middle_coords[i][1]].aspect) || all_aspect_table[grid[middle_coords[i][0]][middle_coords[i][1]].aspect].includes(grid[x][y].aspect)) || all)) {
				middle_coords.splice(i, 1);
				i--;
			}
		}
		return middle_coords;
	} else if (x < Math.floor(grid.length / 2)) {
		for (let i = 0; i < left_coords.length; i++) {

			if (!cell_exists(grid, left_coords[i][0], left_coords[i][1]) || grid[left_coords[i][0]][left_coords[i][1]].aspect == 'none'
				|| !((all_aspect_table[grid[x][y].aspect].includes(grid[left_coords[i][0]][left_coords[i][1]].aspect)
					|| all_aspect_table[grid[left_coords[i][0]][left_coords[i][1]].aspect].includes(grid[x][y].aspect))
					|| all)) {
				left_coords.splice(i, 1);
				i--;
			}
		}
		return left_coords;
	}
	for (let i = 0; i < right_coords.length; i++) {
		if (!cell_exists(grid, right_coords[i][0], right_coords[i][1]) || grid[right_coords[i][0]][right_coords[i][1]].aspect == 'none' || !((all_aspect_table[grid[x][y].aspect].includes(grid[right_coords[i][0]][right_coords[i][1]].aspect) || all_aspect_table[grid[right_coords[i][0]][right_coords[i][1]].aspect].includes(grid[x][y].aspect) || all))) {
			right_coords.splice(i, 1);
			i--;
		}
	}
	return right_coords;
}


function tile_neighbors_wo_aspects(x, y, grid, ignore_barred = true) {
	var left_coords = [[x, y - 1], [x + 1, y], [x + 1, y + 1], [x, y + 1], [x - 1, y], [x - 1, y - 1]];
	var middle_coords = [[x, y - 1], [x + 1, y - 1], [x + 1, y], [x, y + 1], [x - 1, y], [x - 1, y - 1]];
	var right_coords = [[x, y - 1], [x + 1, y - 1], [x + 1, y], [x, y + 1], [x - 1, y + 1], [x - 1, y]];
	if (x == Math.floor(grid.length / 2)) {
		for (let i = 0; i < middle_coords.length; i++) {
			if (!cell_exists(grid, middle_coords[i][0], middle_coords[i][1]) || (grid[middle_coords[i][0]][middle_coords[i][1]].barred && ignore_barred)) {
				middle_coords.splice(i, 1);
				i--;
			}
		}
		return middle_coords;
	} else if (x < Math.floor(grid.length / 2)) {
		for (let i = 0; i < left_coords.length; i++) {
			if (!cell_exists(grid, left_coords[i][0], left_coords[i][1]) || (grid[left_coords[i][0]][left_coords[i][1]].barred && ignore_barred)) {
				left_coords.splice(i, 1);
				i--;
			}
		}
		return left_coords;
	}
	for (let i = 0; i < right_coords.length; i++) {
		if (!cell_exists(grid, right_coords[i][0], right_coords[i][1]) || (grid[right_coords[i][0]][right_coords[i][1]].barred && ignore_barred)) {
			right_coords.splice(i, 1);
			i--;
		}
	}
	return right_coords;
}

/* This function only considers cell neighbors that can be connected */
/* path takes blank cells into account */
/* ignore_aspects doesn't take cells with aspects into account except the starting one */
function neighbor_chain(x, y, grid, path = false, cell_list = null, ignore_aspects = false, not_ignored_id = null, current_path = []) {
	//console.log(`x: ${x}, y: ${y}, cell_list: ${cell_list}`);
	let first_time = false;
	if (cell_list == null) {
		cell_list = [];
		first_time = true;
	}
	if (((grid[x][y].aspect != 'none' || path) && (ignore_aspects ? (grid[x][y].aspect == 'none' || grid[x][y].id == not_ignored_id || first_time) : true)) && !inArray(cell_list, [x, y]) && (current_path.length > 0 ? !current_path.includes(grid[x][y].id) : true)) {
		cell_list.push([x, y]);
		var neighbors;
		if (path) {
			neighbors = tile_neighbors_wo_aspects(x, y, grid);
		} else {
			neighbors = tile_neighbors(x, y, grid);
		}
		//console.log(`neighbors: ${neighbors}`);
		for (let i = 0; i < neighbors.length; i++) {
			//console.log(`i: ${i}, neighbor len: ${neighbors.length}`);
			neighbor_chain(neighbors[i][0], neighbors[i][1], grid, path, cell_list, ignore_aspects, not_ignored_id, current_path);
		}
	}
	return cell_list;
}

function check_continuity(grid) {
	var starting_cell;
	var aspect_cell_counter = 0;
	for (let i = 0; i < grid.length; i++) {
		for (let j = 0; j < grid[i].length; j++) {
			if (grid[i][j].aspect != 'none') {
				if (starting_cell == undefined) {
					starting_cell = [i, j];
				}
				aspect_cell_counter++;
			}
		}
	}
	if (neighbor_chain(starting_cell[0], starting_cell[1], grid).length != aspect_cell_counter) {
		return false;
	}
	return true;
}

function get_cell_by_id(id, grid) {
	for (let i = 0; i < grid.length; i++) {
		for (let j = 0; j < grid[i].length; j++) {
			if (grid[i][j].id == id) {
				return grid[i][j];
			}
		}
	}
}

function id_in_blacklist(id, state) {
	for (let i = 0; i < keylen(state.impossible_closest_cells); i++) {
		if (keyAtIndex(state.impossible_closest_cells, i) == id) {
			return true;
		}
	}
	return false;
}

function aspect_blacklisted_at_id(aspect, id, state) {
	for (let i = 0; i < keylen(state.impossible_closest_cells); i++) {
		if (elemAtIndex(state.impossible_closest_cells, i) == aspect) {
			return true;
		}
	}
	return false;
}

/* Returns whether a cell is usable (aka all aspects haven't been blacklisted on it) */
function impossible_id(id, state) {
	if (id_in_blacklist(id, state)) {
		if (keylen(aspect_table) == state.impossible_closest_cells[id].length) {
			return true;
		}
	}
	return false;
}

function last_used_path_cell(state, path = null) {
	if (!path) {
		path = state.current_path;
	}
	let neighbor_chain_of_path = neighbor_chain(get_cell_by_id(path[0], state.grid).array_x, get_cell_by_id(path[0], state.grid).array_y, state.grid);
	for (let i = 1; i < path.length; i++) {
		if (i == path.length - 1 && !neighbor_chain_of_path.includes(path[i])) {
			return path.length - 2;
		}
		if (get_cell_by_id(path[i], state.grid).aspect == 'none') {
			return i - 1;
		}
	}
	return path.length - 1;
}

/* Checks if two cells are not separated by barred cells, optionnaly separated by aspects */
function in_same_space(id1, id2, grid, aspects = false, current_path = []) {
	let cell1 = get_cell_by_id(id1, grid);
	let cell2 = get_cell_by_id(id2, grid);
	let temp_chain = neighbor_chain(cell1.array_x, cell1.array_y, grid, true, null, aspects, id2, current_path);
	if (inArray(temp_chain, [cell2.array_x, cell2.array_y])) {
		return true;
	}
	return false;
}

function closest_cells(state) {
	/* basically take every cell and get all distances to all other cells */
	/* uses precalculated values */
	/* ignores cells that form a continuity */

	/* EXPERIMENTAL (seems to be working)*/
	if (current_state.current_path && !path_complete(current_state.current_path, current_state)) {
		return [current_state.current_path[last_used_path_cell(current_state)], current_state.current_path[current_state.current_path.length - 1]];
	}
	/* EXPERIMENTAL */

	var current_closest_pair = [];
	for (let i = 0; i < state.grid.length; i++) {
		for (let j = 0; j < state.grid[i].length; j++) {
			/* check that the cell has an aspect and not in blacklist*/
			if (state.grid[i][j].aspect != 'none' && !impossible_id(state.grid[i][j].id, state)) {
				var current_neighbor_chain = neighbor_chain(i, j, state.grid);
				for (let k = 0; k < state.grid.length; k++) {
					/* check if the cell isn't already connected */
					var in_neighbor_chain = false;
					for (let l = 0; l < state.grid[k].length; l++) {
						/*if (state.grid[current_neighbor_chain[l][0]][current_neighbor_chain[l][1]].id == parseInt(keyAtIndex(distance_dict[state.grid[i][j].id], k))) {
							in_neighbor_chain = true;
						}*/
						if (inArray(current_neighbor_chain, [k, l])) {
							in_neighbor_chain = true;
							continue;
						}

						if (state.current_path && !path_complete(current_state.current_path, current_state) && !closest_neighbor_to_cell(state.grid[i][j].id, state.grid[k][l].id, state)) {
							continue;
						}
						if (state.current_path && !path_complete(current_state.current_path, current_state) && !path_complete(state.current_path, state)) {
							if (state.grid[i][j].id != state.current_path[last_used_path_cell(state)]) {
								continue;
							}
						}
						if (!in_neighbor_chain && state.grid[k][l].aspect != 'none' && !impossible_id(state.grid[k][l].id, state) && in_same_space(state.grid[i][j].id, state.grid[k][l].id, state.grid, true)) {
							if (current_closest_pair.length == 0) {
								current_closest_pair = [state.grid[i][j].id, state.grid[k][l].id];
								if (state.current_path && sort_tuple(current_closest_pair, state.current_path)) {
									current_closest_pair = sort_tuple(current_closest_pair, state.current_path);
								}
							} else {
								var current_distance = get_distance(get_cell_by_id(current_closest_pair[0], state.grid).x, get_cell_by_id(current_closest_pair[0], state.grid).y, get_cell_by_id(current_closest_pair[1], state.grid).x, get_cell_by_id(current_closest_pair[1], state.grid).y);
								var potential_distance = get_distance(state.grid[i][j].x, state.grid[i][j].y, state.grid[k][l].x, state.grid[k][l].y);
								if (potential_distance < current_distance) {
									current_closest_pair = [state.grid[i][j].id, state.grid[k][l].id];
									if (state.current_path && sort_tuple(current_closest_pair, state.current_path)) {
										current_closest_pair = sort_tuple(current_closest_pair, state.current_path);
									}
								}
							}
						}
					}
				}
			}
		}
	}
	if (current_closest_pair.length == 0 && state.current_path) {
		return [state.current_path[0], state.current_path[state.current_path.length - 1]];
	}
	return current_closest_pair;
}

function precalculate_cell_distances() {
	console.log("precalculating distances: start");
	for (let i = 0; i < grid.length; i++) {
		for (let j = 0; j < grid[i].length; j++) {
			for (let k = 0; k < grid.length; k++) {
				for (let l = 0; l < grid[k].length; l++) {
					if (!(i == k && j == l)) {
						if (!distance_dict[grid[i][j].id]) {
							distance_dict[grid[i][j].id] = {};
						}
						distance_dict[grid[i][j].id][grid[k][l].id] = get_distance(grid[i][j].x, grid[i][j].y, grid[k][l].x, grid[k][l].y);
					}
				}
			}
		}
	}
	console.log("precalculating distances: done");
}

function closest_neighbor_to_cell(id1, id2, state, path = [], custom_blacklist = [], finding_path = false, current_path = []) {
	var cell1 = get_cell_by_id(id1, state.grid);
	var cell2 = get_cell_by_id(id2, state.grid);
	var temp_neighbors = tile_neighbors_wo_aspects(cell1.array_x, cell1.array_y, state.grid);
	var temp_closest_neighbor = null;
	var temp_closest_distance = null;
	for (let i = 0; i < temp_neighbors.length; i++) {
		let target = state.grid[temp_neighbors[i][0]][temp_neighbors[i][1]];
		if (target.aspect == 'none' && !target.barred && (finding_path ? in_same_space(cell2.id, target.id, state.grid, true, current_path) : true) && ((state.current_path && state.current_path.length > 0) ? target.id == state.current_path[last_used_path_cell(state, state.current_path) + 1] : true)) {
			if (temp_closest_neighbor == null && !custom_blacklist.includes(target.id) && (path.length > 0 == path.includes(target.id)) && (state.current_path_forbidden_child ? state.current_path_forbidden_child[id1] != target.id : true) && (finding_path ? !current_path.includes(target.id) : true)) {
				temp_closest_neighbor = target.id;
				temp_closest_distance = distance_dict[id2][target.id];//distance_dict[target.id][id2];
			} else if ((distance_dict[id2][target.id] < temp_closest_distance) && !custom_blacklist.includes(target.id) && (path.length > 0 == path.includes(target.id)) && (state.current_path_forbidden_child ? state.current_path_forbidden_child[id1] != target.id : true) && (finding_path ? !current_path.includes(target.id) : true)) {
				temp_closest_neighbor = target.id;
				temp_closest_distance = distance_dict[id2][target.id];
			}
		}
	}
	return temp_closest_neighbor;
}

function complexity(aspect, counter = null) {
	if (counter == null) {
		counter = 0;
	}
	counter++;
	if (!aspect) {
		return 0;
	}
	if (all_aspect_table[aspect].length == 1) {
		return 0;
	} else {
		counter += Math.max(complexity(all_aspect_table[aspect][0]), complexity(all_aspect_table[aspect][1]));
	}
	return counter;
}

/* Returns true if an aspect contains another */
function contains(container, contained) {
	if (all_aspect_table[container].length == 1) {
		if (all_aspect_table[container][0] == contained) {
			return 0;
		}
	} else {
		if (all_aspect_table[container][0] == contained) {
			return 0;
		}
		if (all_aspect_table[container][1] == contained) {
			return 1;
		}
	}
	return -1;
}

function can_use_aspect(id, aspect, state) {
	let found = false;
	for (let i = 0; i < keylen(aspect_table); i++) {
		if (keyAtIndex(aspect_table, i) == aspect) {
			found = true;
		}
	}
	if (!found) {
		return false;
	}
	if (state.impossible_closest_cells[id]) {
		for (let i = 0; i < state.impossible_closest_cells[id].length; i++) {
			if (state.impossible_closest_cells[id][i] == aspect) {
				return false;
			}
		}
	}
	return true;
}

function best_aspect(starting_cell_id, target_cell_id, neighbor_cell_id, state) {
	var starting_cell = get_cell_by_id(starting_cell_id, state.grid);
	var target_cell = get_cell_by_id(target_cell_id, state.grid);
	var neighbor_cell = get_cell_by_id(neighbor_cell_id, state.grid);

	var least_complex_aspect = null;
	var current_complexity = null;

	/* small complexity < being linked to target cell */
	if (all_aspect_table[starting_cell.aspect].length == 0) {
		/* primal aspect, must choose compound that includes it */
		/* trying to have smallest complexity */
		/* and then search again to see i we can get something directly related */
		for (let i = 0; i < keylen(aspect_table); i++) {
			if (can_use_aspect(neighbor_cell_id, keyAtIndex(aspect_table, i), state) && least_complex_aspect == null) {
				if (contains(keyAtIndex(aspect_table, i), starting_cell.aspect) > -1) {
					least_complex_aspect = keyAtIndex(aspect_table, i); //aspect_table[starting_cell.aspect][contains(keyAtIndex(aspect_table, i), starting_cell.aspect)];
					current_complexity = complexity(least_complex_aspect);
				}
			} else {
				if (can_use_aspect(neighbor_cell_id, keyAtIndex(aspect_table, i), state) && contains(keyAtIndex(aspect_table, i), starting_cell.aspect) > -1) {
					var temp_aspect = keyAtIndex(aspect_table, i);//aspect_table[starting_cell.aspect][contains(keyAtIndex(aspect_table, i), starting_cell.aspect)];
					if (current_complexity > complexity(temp_aspect)) {
						least_complex_aspect = keyAtIndex(aspect_table, i);//aspect_table[starting_cell.aspect][contains(keyAtIndex(aspect_table, i), starting_cell.aspect)];
						current_complexity = complexity(least_complex_aspect);
					}
				}
			}
		}
		/* trying to match a bit the target aspect */
		if (temp_aspect) {
			for (let i = 0; i < keylen(aspect_table); i++) {
				if (can_use_aspect(neighbor_cell_id, keyAtIndex(aspect_table, i), state) && contains(keyAtIndex(aspect_table, i), starting_cell.aspect) > -1) {
					if ((current_complexity == complexity(temp_aspect) || Math.abs(current_complexity - complexity(temp_aspect)) == 1) && (contains(temp_aspect, target_cell.aspect) || contains(target_cell.aspect, temp_aspect))) {
						least_complex_aspect = temp_aspect;
						current_complexity = complexity(temp_aspect);
					}
				}
			}
		}
	} else if (all_aspect_table[starting_cell.aspect].length == 2) {
		/* compound aspect */
		for (let i = 0; i < keylen(aspect_table); i++) {
			if (least_complex_aspect == null) {
				if (can_use_aspect(neighbor_cell_id, keyAtIndex(aspect_table, i), state) && can_connect(starting_cell.aspect, keyAtIndex(aspect_table, i))) {
					least_complex_aspect = keyAtIndex(aspect_table, i); //aspect_table[starting_cell.aspect][contains(keyAtIndex(aspect_table, i), starting_cell.aspect)];
					current_complexity = complexity(least_complex_aspect);
				}
			} else {
				if (can_use_aspect(neighbor_cell_id, keyAtIndex(aspect_table, i), state) && can_connect(starting_cell.aspect, keyAtIndex(aspect_table, i))) {
					var temp_aspect = keyAtIndex(aspect_table, i);//aspect_table[starting_cell.aspect][contains(keyAtIndex(aspect_table, i), starting_cell.aspect)];
					if (current_complexity > complexity(temp_aspect)) {
						least_complex_aspect = keyAtIndex(aspect_table, i);//aspect_table[starting_cell.aspect][contains(keyAtIndex(aspect_table, i), starting_cell.aspect)];
						current_complexity = complexity(least_complex_aspect);
					}
				}
			}
		}
		if (temp_aspect) {
			for (let i = 0; i < keylen(aspect_table); i++) {
				if (can_use_aspect(neighbor_cell_id, keyAtIndex(aspect_table, i), state) && contains(starting_cell.aspect, keyAtIndex(aspect_table, i)) > -1) {
					if ((current_complexity == complexity(temp_aspect) || Math.abs(current_complexity - complexity(temp_aspect)) == 1) && (contains(temp_aspect, target_cell.aspect) || contains(target_cell.aspect, temp_aspect))) {
						least_complex_aspect = temp_aspect;
						current_complexity = complexity(temp_aspect);
					}
				}
			}
		}
	}
	return least_complex_aspect;
}

/* Blacklists given aspect for given cell and state */
function blacklist_aspect(id, aspect, state) {
	if (!state.impossible_closest_cells[id]) {
		state.impossible_closest_cells[id] = [];
	}
	if (state.impossible_aspects_for_current_path == null) {
		state.impossible_aspects_for_current_path = {};
	}
	if (!state.impossible_aspects_for_current_path.hasOwnProperty(id)) {
		state.impossible_aspects_for_current_path[id] = [];
	}
	state.impossible_closest_cells[id].push(aspect);
	state.impossible_aspects_for_current_path[id].push(aspect);
	state.impossible_aspects_for_current_path[id] = remove_dupes(state.impossible_aspects_for_current_path[id]);
}

function arrays_equal(a, b) {
	if (a.length != b.length) {
		return false;
	}
	for (let i = 0; i < a.length; i++) {
		if (a[i] != b[i]) {
			return false;
		}
	}
	return true;
}

function occurence_in_array(array, value) {
	let counter = 0;
	for (let i = 0; i < array.length; i++) {
		if (array[i] == value) {
			counter++;
		}
	}
	return counter;
}

function sort_tuple(tuple, pattern_array) {
	if (tuple.length != 2 || pattern_array.length < 2) {
		return null;
	}
	if (!in_normal_array(pattern_array, tuple[0]) || !in_normal_array(pattern_array, tuple[1])) {
		return null;
	}
	if (pattern_array.indexOf(tuple[0]) < pattern_array.indexOf(tuple[1])) {
		return tuple;
	}
	return [tuple[1], tuple[0]];
}

function arrays_same_content(a, b) {
	if (a.length != b.length) {
		return false;
	}
	for (let i = 0; i < a.length; i++) {
		if (occurence_in_array(a, a[i]) != occurence_in_array(b, a[i])) {
			return false;
		}
	}
	return true;
}

function already_blacklisted_cells_from_path(blacklisted_path, blacklisted_cells) {
	let counter = 0;
	for (let i = 0; i < blacklisted_path.length; i++) {
		if (blacklisted_cells[i] && blacklisted_cells[i] == blacklisted_path[i]) {
			counter++;
		}
	}
	return counter;
}

/* Finds shortest path from one cell to another considering blacklisted paths */
function find_path(id1, id2, state) {
	//let res = [];
	/*state.blacklisted_paths.push([0, 1, 2, 3]);
	state.blacklisted_paths.push([0, 1, 6, 2, 3]);
	state.blacklisted_paths.push([0, 1, 6, 7, 3]);*/

	let cell1 = get_cell_by_id(id1, state.grid);
	let cell2 = get_cell_by_id(id2, state.grid);
	let found = false;
	let depth_limit = 100000;
	let current_depht = 0;
	let path_return_from_bad_state = false;
	let path_state_history = [];
	class Path_state {
		constructor(starting_cell_id) {
			//'parent':'blacklisted child'
			this.blacklist_dict = {};
			this.current_path = [starting_cell_id];
		}
	}
	let original_path = new Path_state(id1);
	let current_path_state = new Path_state(id1);
	while (!found) {
		current_depht++;
		if (current_depht == depth_limit) {
			return null;
		}
		if (!path_return_from_bad_state) {
			let cell_to_append = null;
			if (current_path_state.blacklist_dict.hasOwnProperty(current_path_state.current_path[current_path_state.current_path.length - 1])) {
				cell_to_append = get_cell_by_id(closest_neighbor_to_cell(current_path_state.current_path[current_path_state.current_path.length - 1], id2, state, [], current_path_state.blacklist_dict[current_path_state.current_path[current_path_state.current_path.length - 1]].concat(current_path_state.current_path), true, current_path_state.current_path), state.grid);
			} else {
				cell_to_append = get_cell_by_id(closest_neighbor_to_cell(current_path_state.current_path[current_path_state.current_path.length - 1], id2, state, [], [], true, current_path_state.current_path), state.grid);
			}
			if (cell_to_append) {
				current_path_state.current_path.push(cell_to_append.id);
				path_state_history.push(deepcopy(current_path_state));

				let grid_copy = deepcopy(state.grid);
				for (let i = 0; i < grid_copy.length; i++) {
					for (let j = 0; j < grid_copy[i].length; j++) {
						if (!current_path_state.current_path.includes(grid_copy[i][j].id) && grid_copy[i][j].id != id2) {
							grid_copy[i][j].barred = true;
						}
					}
				}
				if (inArray(neighbor_chain(cell1.array_x, cell1.array_y, grid_copy, true), [cell2.array_x, cell2.array_y])) {
					current_path_state.current_path.push(id2);
					let same = false;
					for (let i = 0; i < state.blacklisted_paths.length; i++) {
						if (arrays_equal(state.blacklisted_paths[i], current_path_state.current_path)) {
							same = true;
							path_return_from_bad_state = true;
							break;
						}
					}
					if (!same) {
						return current_path_state.current_path;
					}
				}
			} else {
				path_return_from_bad_state = true;
			}
		} else {
			// path_state_history.pop();
			// if (path_state_history.length == 0) {
			// 	path_state_history.push(deepcopy(original_path));
			// }
			//if (path_state_history[path_state_history.length - 1].blacklist_dict[current_path_state.current_path[current_path_state.current_path.length - 3]] == undefined) {
			//	path_state_history[path_state_history.length - 1].blacklist_dict[current_path_state.current_path[current_path_state.current_path.length - 3]] = [];
			//}

			if (path_state_history[path_state_history.length - 1].blacklist_dict[current_path_state.current_path[current_path_state.current_path.length - 2]] == undefined) {
				path_state_history[path_state_history.length - 1].blacklist_dict[current_path_state.current_path[current_path_state.current_path.length - 2]] = [];
			}
			path_state_history[path_state_history.length - 1].blacklist_dict[current_path_state.current_path[current_path_state.current_path.length - 2]].push(current_path_state.current_path[current_path_state.current_path.length - 1]);


			//path_state_history[path_state_history.length - 1].blacklist_dict[current_path_state.current_path[current_path_state.current_path.length - 3]].push(current_path_state.current_path[current_path_state.current_path.length - 2]);



			current_path_state = deepcopy(path_state_history[path_state_history.length - 1]);
			path_state_history.pop();
			if (path_state_history.length == 0) {
				path_state_history.push(deepcopy(original_path));
			}
			path_return_from_bad_state = false;
		}
	}
}

/* Removes dupes form an array */
function remove_dupes(array) {
	let res = [];
	for (let i = 0; i < array.length; i++) {
		if (!res.includes(array[i])) {
			res.push(array[i]);
		}
	}
	return res;
}

/* Returns true if every aspect has been tried for the path */
function every_aspect_tried_for_path(path, state) {
	let grid = state.grid;
	let possible_aspects = [[get_cell_by_id(path[0], grid).aspect]];
	let possible_aspects_dict = {};
	for (let i = 1; i < path.length - 1; i++) {
		possible_aspects.push([]);
		for (let j = 0; j < keylen(aspect_table); j++) {
			for (let k = 0; k < possible_aspects[i - 1].length; k++) {
				if (can_connect(possible_aspects[i - 1][k], keyAtIndex(aspect_table, j))) {
					possible_aspects[i].push(keyAtIndex(aspect_table, j));
				}
			}
		}
		possible_aspects[i] = remove_dupes(possible_aspects[i]);
		possible_aspects_dict[path[i]] = possible_aspects[i];
	}

	//
	// debug ///state.impossible_closest_cells = possible_aspects_dict;
	//

	//return possible_aspects_dict;
	if (keylen(state.impossible_closest_cells) == 0) {
		return false;
	}
	for (let i = 0; i < keylen(state.impossible_closest_cells); i++) {
		if (!possible_aspects_dict[keyAtIndex(state.impossible_closest_cells, i)]) {
			return false;
		}
		if (state.impossible_closest_cells[keyAtIndex(state.impossible_closest_cells, i)].length != possible_aspects_dict[keyAtIndex(state.impossible_closest_cells, i)].length) {
			return false;
		}
	}
	return true;
}

function path_complete(path, state) {
	let first_path_cell = get_cell_by_id(path[0], state.grid);
	let neighbors = neighbor_chain(first_path_cell.array_x, first_path_cell.array_y, state.grid);
	for (let i = 0; i < neighbors.length; i++) {
		neighbors[i] = state.grid[neighbors[i][0]][neighbors[i][1]].id;
	}
	//if (arrays_same_content(path, neighbors)) {
	if (array_contains_array(neighbors, path)) {
		return true;
	}
	return false;
}

function remove_excess_aspects(state) {
	for (let i = 0; i < state.current_path.length - 2; i++) {
		let cell1 = get_cell_by_id(state.current_path[i], state.grid);
		let cell2 = get_cell_by_id(state.current_path[i + 1], state.grid);
		let cell3 = get_cell_by_id(state.current_path[i + 2], state.grid);
		if (cell1.aspect == 'none' || cell2.aspect == 'none' || cell3.aspect == 'none') {
			continue;
		}
		let cell1_neighbors = tile_neighbors(cell1.array_x, cell1.array_y, state.grid, true);
		let are_neighbors = false;
		for (let j = 0; j < cell1_neighbors.length; j++) {
			if (cell3.id == state.grid[cell1_neighbors[j][0]][cell1_neighbors[j][1]].id) {
				are_neighbors = true;
			}
		}
		if (are_neighbors && can_connect(cell1.aspect, cell3.aspect)) {
			cell2.aspect = 'none';//cell3.aspect = 'none';

		}
	}
	for (let i = 1; i < state.current_path.length - 1; i++) {
		let cell1 = get_cell_by_id(state.current_path[i], state.grid);
		if (cell1.aspect == 'none') {
			continue;
		}
		let cell_neighbors = tile_neighbors(cell1.array_x, cell1.array_y, state.grid, true);
		let connected_count = 0;
		for (let j = 0; j < cell_neighbors.length; j++) {
			if (can_connect(cell1.aspect, state.grid[cell_neighbors[j][0]][cell_neighbors[j][1]].aspect)) {
				connected_count++;
			}
		}
		if (connected_count == 1) {
			cell1.aspect = 'none';
		}
	}
}

var debug_depth = 47;

function main_loop() {
	if (depth == debug_depth) {
		console.log("debug");
	}
	console.log(`entering depth ${depth}`);
	depth++;
	console.log(current_state.bad_state_reason)
	if (check_continuity(current_state.grid)) {
		solved = true;
		console.log("==============\nS O L V E D\n==============");
		return;
	}
	if (!returning_from_bad_state) {
		console.log(`Not returning from bad state`)
		
		var temp_closest_cells = closest_cells(current_state);
		console.log(temp_closest_cells);

		if (temp_closest_cells.length > 0) {
			if (current_state.current_path && path_complete(current_state.current_path, current_state)) {
				remove_excess_aspects(current_state);
				current_state.current_path = null;
			} else {
				if (!current_state.current_path) {
					current_path = find_path(temp_closest_cells[0], temp_closest_cells[1], current_state);
					current_state.current_path = deepcopy(current_path);
				}
				var first_cell = get_cell_by_id(temp_closest_cells[0], current_state.grid);
				var second_cell = get_cell_by_id(temp_closest_cells[1], current_state.grid);
				if (current_state.current_path) {
					current_state.last_move = [first_cell.id, second_cell.id];
					var closest_neighbor = get_cell_by_id(closest_neighbor_to_cell(first_cell.id, second_cell.id, current_state, current_state.current_path), current_state.grid);
					if (closest_neighbor) {
						var temp_best_aspect = best_aspect(first_cell.id, second_cell.id, closest_neighbor.id, current_state);
						if (temp_best_aspect) {
							closest_neighbor.aspect = temp_best_aspect;
							current_state.last_cell = closest_neighbor.id;
							grid = current_state.grid;
							state_history.push(deepcopy(current_state));
						} else {
							returning_from_bad_state = true;
							if (first_cell.id == current_state.current_path[0]) {
								current_state.bad_state_reason = 'invalid_path';
							} else {
								current_state.bad_state_reason = 'no_aspect';
							}
						}
					} else {
						returning_from_bad_state = true;
						current_state.bad_state_reason = 'no_neighbor';
					}
				} else {
					returning_from_bad_state = true;
					current_state.bad_state_reason = 'no_path';
				}
			}
		} else {
			returning_from_bad_state = true;
			current_state.bad_state_reason = 'no_closest_cells';
		}
	} else {
		console.log(`Returning from bad state`)

		if (current_state.bad_state_reason == 'no_aspect') {
			//Means that all possible aspects for a cell in a path are blacklisted
			//Fix: blacklist last added aspect and go back

			state_history.pop();
			if (state_history.length <= 0) {
				current_state.current_path = null;
				state_history.push(deepcopy(original_state));
			}
			let cell_to_blacklist = get_cell_by_id(current_state.last_cell, current_state.grid);
			blacklist_aspect(cell_to_blacklist.id, cell_to_blacklist.aspect, state_history[state_history.length - 1]);
			current_state = deepcopy(state_history[state_history.length - 1]);

		} else if (current_state.bad_state_reason == 'no_neighbor') {
			//Usually means that the path has been filled and it's wrong
			//Fix: blacklist last added aspect and go back

			state_history.pop();
			if (state_history.length <= 0) {
				current_state.current_path = null;
				state_history.push(deepcopy(original_state));
			}
			let cell_to_blacklist = get_cell_by_id(current_state.last_cell, current_state.grid);
			blacklist_aspect(cell_to_blacklist.id, cell_to_blacklist.aspect, state_history[state_history.length - 1]);
			current_state = deepcopy(state_history[state_history.length - 1]);

		} else if (current_state.bad_state_reason == 'no_path') {

		} else if (current_state.bad_state_reason == 'no_closest_cells') {
			//sure, that's pretty bad
		} else if (current_state.bad_state_reason == 'invalid_path') {
			current_state.blacklisted_paths.push(current_state.current_path);
			if (state_history.length == 0) {
				return;
			}
			state_history[state_history.length - 1].blacklisted_paths.push(current_state.current_path);
			current_state.current_path = null;
			current_state.impossible_aspects_for_current_path = {};
			current_state.impossible_closest_cells = {};
		}
		returning_from_bad_state = false;
	}
}

var current_state = new State(grid);
var depth = 0;
var state_history = [];
var original_grid;
var original_state;
var returning_from_bad_state = false;
var last_touched_cells = null;
var chosen_cell = null;
var considered_cells = null;
var solved = false;
var max_depth = 10000;
var current_path = null;



function solve() {
	current_state = new State(grid);
	depth = 0;
	state_history = [];
	original_grid;
	original_state;
	returning_from_bad_state = false;
	last_touched_cells = null;
	chosen_cell = null;
	considered_cells = null;
	solved = false;
	max_depth = 10000;
	current_path = null;
	precalculate_cell_distances();
	original_grid = deepcopy(grid);
	original_state = new State(original_grid);
	let found_aspects = 0;
	for (let i = 0; i < grid.length; i++) {
		for (let j = 0; j < grid[i].length; j++) {
			if (grid[i][j].aspect != 'none') {
				grid[i][j].base = true;
				found_aspects++;
			}
		}
	}
	if (found_aspects > 1) {
		while (!solved && depth < max_depth) {
			main_loop();
		}

	} else {
		console.log("something went wrong")
	}
}

function draw_initial_grid(grid_size) {

	// let hex_side_len = canvas.height / 2 - 1;
	////draw_hexagon(canvas.width / 2, canvas.height / 2, hex_side_len, undefined, undefined, undefined, true);

	//var hex_num_vertical = grid_size * 2 - 1;
	//var blank_num_vertical = hex_num_vertical - 1;
	////console.debug("hex_num_vertical: " + hex_num_vertical + ", blank_num_vertical: " + blank_num_vertical)
	//let blank_ratio = 1/10;
	//let blank_len = Math.floor(canvas.height * blank_ratio / (hex_num_vertical * blank_ratio + blank_num_vertical));

	//let small_hex_len_vertical = canvas.height / (hex_num_vertical + blank_num_vertical * blank_ratio);//canvas.height / hex_num_vertical - blank_len / hex_num_vertical;
	//let small_hex_len = small_hex_len_vertical / 2 / Math.cos(to_rad(30));

	//console.debug(`blank_len: ${blank_len}, small_hex_len_vertical: ${small_hex_len_vertical}, small_hex_len: ${small_hex_len}`);

	/*for (let i = 0; i < hex_num_vertical; i++) {
		draw_hexagon(canvas.width / 2, i * (small_hex_len_vertical + blank_len) + small_hex_len_vertical / 2, small_hex_len, 2);
	}*/


	/* the number of "columns" is also the number of tiles in the middle column */
	/* the column in the middle has grid_size * 2 - 1 tiles */
	column_num = grid_size * 2 - 1;
	let list = [];
	let id = 0;
	for (let i = 0; i < column_num; i++) {
		list.push([]);
		/* number of tiles in column is grid_size + i, valid up to the middle tho.*/
		let num_of_cells_in_column;
		let shift_x;
		if (i <= Math.floor(column_num / 2)) {
			num_of_cells_in_column = grid_size + i;
			// shift_x = (column_num - num_of_cells_in_column) * (1.5 * small_hex_len + blank_len);//(column_num - num_of_cells_in_column) * (small_hex_len + blank_len);
		} else {
			num_of_cells_in_column = column_num - i + grid_size - 1;
			// shift_x = -(column_num - num_of_cells_in_column) * (1.5 * small_hex_len + blank_len);
		}
		//console.debug("num of cells: " + num_of_cells_in_column);

		/* each row going from the center, is shifted by half a hexagone per row */
		// let shift_y = (column_num - num_of_cells_in_column) * (small_hex_len_vertical / 2 + blank_len / 2);//(column_num - num_of_cells_in_column) * small_hex_len_vertical / 2 + blank_len / 2;


		for (let j = 0; j < num_of_cells_in_column; j++) {
			//draw_hexagon(canvas.width / 2 - shift_x, j * (small_hex_len_vertical + blank_len) + small_hex_len_vertical / 2 + shift_y, small_hex_len, 2);
			list[i].push(new Cell(0, 0, 'none', false, 0, id, i, j));
			id++;
		}
	}
	return list;
}
function reset() {
	//grid = draw_initial_grid(5);

	depth = 0;
	state_history = [];
	original_grid;
	original_state;
	returning_from_bad_state = false;
	last_touched_cells = null;
	chosen_cell = null;
	considered_cells = null;
	solved = false;
	max_depth = 10000;
	current_path = null;
	// Assuming grid is a 2D array: grid[row][col]


}


function generateGrid(data) {
  const hexEntries = data.hexEntries;
  if (!hexEntries) return [];

  // Extract and process coordinates from hexEntries
  const keys = Object.keys(hexEntries).map(k => k.split(':').map(Number));
  const xs = keys.map(k => k[0]);
  const ys = keys.map(k => k[1]);

  const minX = Math.min(...xs);
  const maxX = Math.max(...xs);
  const minY = Math.min(...ys);
  const maxY = Math.max(...ys);

  const width = maxX - minX + 1;
  const height = maxY - minY + 1;

  // Calculate the size parameter (radius of the hex grid)
  const size = Math.floor((width - 1) / 2);

  let idCounter = 0;

  const Cells = Array.from({ length: width }, (_, array_x) => {
    // Calculate how many cells should be in this column
    const colSize = size * 2 + 1 - Math.abs(array_x - size);

    return Array.from({ length: colSize }, (_, array_y) => {
      // Determine the actual y coordinate in the original data
      let y;
      if (array_x <= size) {
        // Left side columns: bottom-aligned
        y = maxY - colSize + array_y + 1;
      } else {
        // Right side columns: top-aligned
        y = minY + array_y;
      }

      const x = array_x + minX;
      const key = `${x}:${y}`;
      const cellData = hexEntries[key];

      const aspect = cellData ? cellData.aspect.toLowerCase() : 'none';
	  barred = true
	  if(cellData){
		barred = false
	  }

	  
      return {
        x,
        y,
        aspect,
        barred: barred,
        side_len: 0,
        selected: false,
        highlighted: false,
        base: aspect !== 'none', // ✅ base true if aspect is not "none"
        id: idCounter++,
        array_x,
        array_y
      };
    });
  });

  return Cells;
}

function generateColumnHeights(gridB) {
  const cols = gridB.length;
  const center = Math.floor(cols / 2);
  const maxRows = gridB[center].length;
  
  return gridB.map((_, i) => maxRows - Math.abs(center - i));
}

function remapGridBIndices(gridB) {
  const center = Math.floor(gridB.length / 2);
  const columnHeights = generateColumnHeights(gridB);

  return gridB.map((col, array_x) => {
    const height = columnHeights[array_x];
    let startIndex;
    
    if (array_x <= center) {
      // Left columns: take bottom-aligned slice
      startIndex = col.length - height;
    } else {
      // Right columns: take top-aligned slice
      startIndex = 0;
    }
    
    return col.slice(startIndex, startIndex + height).map((cell, newY) => ({
      ...cell,
      array_x,
      array_y: newY
    }));
  });
}
const http = require("http");

const server = http.createServer((req, res) => {
	if (req.method === "POST") {
		let body = "";

		// Collect body chunks
		req.on("data", chunk => {
			body += chunk.toString();
		});

		// When finished
		req.on("end", () => {
			console.log("Received body:", body);

			let data;
			try {
				data = JSON.parse(body);  // parse first
			} catch (e) {
				data = { raw: body };
			}

			// Now pass the parsed object to generateGrid
			grid = generateGrid(data);
			grid = remapGridBIndices(grid)
			const fs = require("fs");

			// Suppose `grid` is your gridData
			console.log("grid:", grid);

			// Write to file
			fs.writeFileSync("grid.json", JSON.stringify(grid, null, 2), "utf-8");

			console.log("Saved to grid.json");

			current_state.grid = grid;
			reset();
			solve();


			// Respond
			res.writeHead(200, { "Content-Type": "application/json" });
			res.end(JSON.stringify(grid));
		});


	} else {
		// For other request types
		res.writeHead(405, { "Content-Type": "text/plain" });
		res.end("Only POST supported");
	}
});

server.listen(3000, () => {
	console.log("Server running at http://localhost:3000");
});


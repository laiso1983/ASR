
var selectedNode = "root";              // Keep track of which tree node is selected
var tree; // global tree object for updating parameters, etc

/*
** Function to set the tree node colours; selected node is a different colour to other nodes
*/
/*var node_colorizer = function(element, node) {
    if (node.name == selectedNode) {
        element.select("circle").style('fill', "hsl(219,70%,80%)");
    } else {
        element.select("circle").style('fill', "hsl(211,10%,80%)");
    }
};*/

/*
** Function to set up the phylogenetic tree structure (options and menu items)
*//*
var setup_tree = function(tree_div, newick_string) {
    tree = d3.layout.phylotree()
                    .svg (d3.select(tree_div))
                    .options({
                         'selectable': false,
                         'collapsible': true})
                    .radial(false)
                    .node_circle_size(6)
                    .style_nodes(node_colorizer);
    tree(d3_phylotree_newick_parser(newick_string)).layout();

    // add a custom menu for (in this case) terminal nodes
    tree.get_nodes().forEach (function (node) {
        d3_add_custom_menu (node, // add to this node
      	        function(node) {return("Create marginal reconstruction")},
      		    function () {
      		        perform_marginal(node);
      		    }
     	 	);
     	d3_add_custom_menu (node, // add to this node
      		function(node) {return("View joint reconstruction results");},
      			 function () {
                     displayJointGraph(node);
                 }
            );
    });
};*/

/*
** Perform marginal reconstruction of the selected tree node
*/
var perform_marginal = function(node_name, node_fill) {
    $("#progress").removeClass("disable");
    selectedNode = node_name;
    inferType = "marginal";
    $.ajax({
        url : window.location,
        type : 'POST',
        data : {infer: inferType, node: selectedNode},
        success: function(data) {
            json_str = data;
            graph_array = [];
            //add_new_poag(json_str, node_name, node_fill);
            // if mutant library is selected, display mutant library with the selected number of mutants, else just
            // display the marginal distribution in the nodes
            if ($("#mutant-btn").attr("aria-pressed") === 'true') {
                set_draw_mutants(true);
                $('#mutant-input').fadeIn();
                view_mutant_library($('#text-mutant').val());
            } else {
                set_draw_mutants(false);
                $('#mutant-input').fadeOut();
                view_marginal();
            }
            refresh_elements();
        }
    });
    $("#progress").addClass("disable");
};

/*
** Refresh the results view to show joint reconstruction results of the selected tree node
*/
var displayJointGraph = function(node_name, node_fill) {
    selectedNode = node_name;
    var resetGraphs = false;
    if (inferType == "marginal") {
        resetGraphs = true;
    }
    inferType = "joint";
    $.ajax({
        url : window.location,
        type : 'POST',
        data : {infer: inferType, node: selectedNode},
        success: function(data) {
            json_str = data;
            drawMutants = false;
            //problem below, this only colours for the second poag, leaving the colour for 'poag1'
            //undefined, hence why it comes up black for the fused nodes -> I may have fixed this
            setup_poags(json_str, false, false, false, node_name)
            graph_array.push(JSON.parse(json_str));
            poags.options.poagColours["poag" + (Object.keys(poags.options.poagColours).length+1)] = node_fill;
            poags.options.name_to_merged_id[node_name] = ["poag" + (Object.keys(poags.options.poagColours).length+1)];
            poags.options.names_to_colour[node_name] = node_fill;

            var new_graph = fuse_multipleGraphs(graph_array);
            setup_poags(new_graph, false, false, true, 'Merged')

        }
    });
    $("#progress").addClass("disable");
};

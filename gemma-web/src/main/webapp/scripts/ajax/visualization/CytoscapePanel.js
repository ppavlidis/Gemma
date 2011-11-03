Ext.namespace('Gemma');

Gemma.CytoscapePanel = Ext.extend(
Ext.Panel, {

    title: 'Cytoscape',

    layoutIndex: 0,

    currentResultsStringency: 2,

    currentNodeGeneIds: [],

    currentQueryGeneIds: [],

    //used to pass a subset of results that meet the stringency threshold to the coexpressionGrid widget displaying the results in a table
    trimmedKnownGeneResults: [],

    //used to apply a filter to the graph to remove nodes when stringency changes
    trimmedNodeIds: [],

    ready: false,

    dataJSON: {
        nodes: [],
        edges: []
    },

    dataSchemaJSON: {

        nodes: [{
            name: 'label',
            type: 'string'
        }, {
            name: 'geneid',
            type: 'number'
        }, {
            name: 'queryflag',
            type: 'boolean'
        }, {
            name: 'nodeDegree',
            type: 'number'
        }, {
            name: 'nodeDegreeBin',
            type: 'number'
        }, {
            name: 'officialName',
            type: 'string'
        }, {
            name: 'ncbiId',
            type: 'string'

        }

        ],
        edges: [{
            name: 'positivesupport',
            type: 'number'
        }, {
            name: 'negativesupport',
            type: 'number'
        }, {
            name: 'support',
            type: 'number'
        }, {
            name: 'supportsign',
            type: 'string'
        }, {
            name: 'nodeDegreeBin',
            type: 'number'
        }]

    },

    options: {
        // where you have the Cytoscape Web SWF
        swfPath: "/Gemma/scripts/cytoscape/swf/CytoscapeWeb",
        // where you have the Flash installer SWF
        flashInstallerPath: "/Gemma/scripts/cytoscape/swf/playerProductInstall"
    },

    nodeDegreeVisualStyleFlag: true,

    visualization: {},

    visual_style_regular: {
        global: {
            backgroundColor: "#FFF7FB"
        },
        nodes: {
            tooltipText: "Official Name:${officialName}<br/>Node Degree:${nodeDegree}<br/>NCBI Id:${ncbiId}<br/>",
            shape: "ELLIPSE",
            borderWidth: {
                discreteMapper: {
                    attrName: "queryflag",
                    entries: [{
                        attrValue: true,
                        value: 3
                    }, {
                        attrValue: false,
                        value: 0
                    }]
                }


            },

            size: {
                defaultValue: 30

            },

            labelFontColor: "#252525",

            labelHorizontalAnchor: "center",
            borderColor: {
                discreteMapper: {
                    attrName: "queryflag",
                    entries: [{
                        attrValue: true,
                        value: "#E41A1C"
                    }, {
                        attrValue: false,
                        value: "#6BAED6"
                    }]
                }

            },
            color: "#5E38F5"
        },
        edges: {
            tooltipText: "Edge Nodes: ${target} to ${source}<br/>Positive Support:${positivesupport}<br/>Negative Support:${negativesupport}",
            width: {
                defaultValue: 1,
                continuousMapper: {
                    attrName: "support",
                    minValue: 1,
                    maxValue: 6 /*, minAttrValue: 0.1, maxAttrValue: 1.0*/
                }

            },

            color: {


                discreteMapper: {
                    attrName: "supportsign",
                    entries: [{
                        attrValue: "both",
                        value: "#FDDBC7"
                    }, {
                        attrValue: "positive",
                        value: "#E41A1C"
                    }, {
                        attrValue: "negative",
                        value: "#4D4D4D"
                    }]
                }
            }
        }
    },
    visual_style_node_degree: {
        global: {
            backgroundColor: "#FFF7FB"
        },
        nodes: {
            tooltipText: "Official Name:${officialName}<br/>Node Degree:${nodeDegree}<br/>NCBI Id:${ncbiId}<br/>",
            shape: "ELLIPSE",
            borderWidth: {
                discreteMapper: {
                    attrName: "queryflag",
                    entries: [{
                        attrValue: true,
                        value: 3
                    }, {
                        attrValue: false,
                        value: 0
                    }]
                }


            },

            size: {
                defaultValue: 30

            },

            labelFontColor: {
                continuousMapper: {
                    attrName: "nodeDegreeBin",
                    minValue: "#252525",
                    maxValue: "#BDBDBD",
                    maxAttrValue: 11

                }

            },

            labelHorizontalAnchor: "center",
            borderColor: {
                discreteMapper: {
                    attrName: "queryflag",
                    entries: [{
                        attrValue: true,
                        value: "#E41A1C"
                    }, {
                        attrValue: false,
                        value: "#FFF7FB"
                    }]
                }

            },
            color: {

                continuousMapper: {
                    attrName: "nodeDegreeBin",
                    minValue: "#3333FF",
                    //"#43A2CA",
                    maxValue: "#FFF7FB",
                    //minAttrValue: 1 //,
                    maxAttrValue: 11
                }
            }
        },
        edges: {
            tooltipText: "Edge Nodes: ${target} to ${source}<br/>Positive Support:${positivesupport}<br/>Negative Support:${negativesupport}",
            width: {
                defaultValue: 1,
                continuousMapper: {
                    attrName: "support",
                    minValue: 1,
                    maxValue: 6 /*, minAttrValue: 0.1, maxAttrValue: 1.0*/
                }

            },
            //TEST OUT OPACITY
            opacity: {

                customMapper: {
                    functionName: "edgeOpacityMapper"
                }

            },

            color: {


                discreteMapper: {
                    attrName: "supportsign",
                    entries: [{
                        attrValue: "both",
                        value: "#4575B4"
                    }, {
                        attrValue: "positive",
                        value: "#762A83"
                    }, {
                        attrValue: "negative",
                        value: "#1B7837"
                    }]
                }
            }


        }
    },





    initComponent: function () {

        var vis = new org.cytoscapeweb.Visualization("cytoscapeweb", this.options);

        vis["edgeOpacityMapper"] = function (data) {

            if (data["nodeDegreeBin"] == null) {
                return 0.05;

            }

            return 1.05 - data["nodeDegreeBin"] / 10;

        };


        Ext.apply(
        this, {
            tbar: [{
                xtype: 'tbtext',
                text: 'Stringency:'

            },

            {
                xtype: 'tbspacer'
            }, {
                xtype: 'spinnerfield',
                itemId: 'stringencySpinner',
                decimalPrecision: 1,
                incrementValue: 1,
                accelerate: false,
                allowBlank: false,
                allowDecimals: false,
                allowNegative: false,
                minValue: Gemma.MIN_STRINGENCY,
                maxValue: 999,
                fieldLabel: 'Stringency ',
                value: 2,
                width: 60,
                fieldTip: "The minimum number of datasets that must show coexpression for a result to appear"

            },
/*
            '->',

            {
                xtype: 'combo',
                itemId: 'exportCombo',
                emptyText: 'Export As ...',
                fieldLabel: 'exportas',
                store: new Ext.data.SimpleStore({
                    fields: ['exportas'],
                    data: [
                        ['PNG'],
                        ['SVG'],
                        ['PDF'],
                        ['XGMML'],
                        ['GRAPHML'],
                        ['SIF']
                    ]
                }),
                displayField: 'exportas',
                mode: 'local',
                hidden : true

            },
*/
            '->',

            {
                xtype: 'button',
                itemId: 'changeLayout',
                text: 'Change Layout',


            },

            '->',

            {
                xtype: 'button',
                itemId: 'nodeDegreeEmphasis',
                text: 'Node Degree Emphasis',
                enableToggle: 'true',
                pressed: 'true'

            },

            ],
            //end tbar
            margins: {
                top: 0,
                right: 0,
                bottom: 0,
                left: 0
            },

            items: [

            {

                xtype: 'flash',
                height: 555,
                width: 898,

                id: 'cytoscapeweb',
                listeners: {
                    afterrender: {
                        scope: this,
                        fn: function () {


                            if (!this.loadMask) {
                                this.loadMask = new Ext.LoadMask(this.getEl(), {
                                    msg: "Searching for analysis results ...",
                                    msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
                                });
                            }
                            this.loadMask.show();


                            this.currentResultsStringency = this.coexCommand.stringency;

                            this.currentNodeGeneIds = [];

                            this.currentQueryGeneIds = [];


                            //todo just pass in results object from form search to avoid below
                            var results = {};

                            Ext.apply(results, {
                                queryGenes: this.queryGenes,
                                knownGeneResults: this.knownGeneResults
                            });
                            results.queryGenes = this.queryGenes;
                            results.knownGeneResults = this.knownGeneResults;

                            this.currentResultsStringency = this.coexCommand.stringency;
                            this.initialCoexSearchCallback(results);



                        }
                    }

                }

            },

            ]
        });


        vis.panelRef = this;

        vis.ready(function () {

            vis.nodeTooltipsEnabled(true);

            vis.edgeTooltipsEnabled(true);

            vis.addContextMenuItem("Extend this node", "nodes", function (evt) {

                var rootNode = evt.target;


                if (vis.panelRef.currentQueryGeneIds.indexOf(rootNode.data.geneid) === -1) {


                    if (vis.panelRef.currentQueryGeneIds.length < Gemma.MAX_GENES_PER_CLASSIC_COEX_QUERY) {

                        Ext.apply(
                        vis.panelRef.coexCommand, {
                            geneIds: [rootNode.data.geneid],
                            queryGenesOnly: false
                        });


                        vis.panelRef.currentQueryGeneIds.push(rootNode.data.geneid);


                        vis.panelRef.updateSearchFormGenes(vis.panelRef.currentQueryGeneIds);

                        vis.panelRef.loadMask.show();

                        //do all new searches at stringency 2 so set the spinner value
                        var spinner = vis.panelRef.getTopToolbar().getComponent('stringencySpinner');
                        spinner.setValue(2);

                        ExtCoexpressionSearchController.doSearchQuick2(
                        vis.panelRef.coexCommand, {
                            callback: vis.panelRef.extendThisNodeInitialCoexSearchCallback.createDelegate(vis.panelRef)

                        });

                    } else {

                        Ext.Msg.alert('Status of Search', 'This graph already has a max of ' + Gemma.MAX_GENES_PER_CLASSIC_COEX_QUERY + ' query genes. It cannot be extended');

                    }

                } else {

                    Ext.Msg.alert('Status of Search', 'This Node has already been extended');

                }

            });
            vis.addContextMenuItem("Extend Selected nodes", "none", function (evt) {

                var selectedNodes = vis.selected("nodes");

                if (selectedNodes.length > 0 && selectedNodes.length <= Gemma.MAX_GENES_PER_CLASSIC_COEX_QUERY) {


                    if (vis.panelRef.currentQueryGeneIds.length + selectedNodes.length <= Gemma.MAX_GENES_PER_CLASSIC_COEX_QUERY) {

                        //do all new searches at stringency 2 so set the spinner value
                        var spinner = vis.panelRef.getTopToolbar().getComponent('stringencySpinner');
                        spinner.setValue(2);

                        var extendedNodesGeneIdArray = [];
                        var sNodesLength = selectedNodes.length;

                        var i;
                        for (i = 0; i < sNodesLength; i++) {
                            extendedNodesGeneIdArray[i] = selectedNodes[i].data.geneid;

                            if (vis.panelRef.currentQueryGeneIds.indexOf(selectedNodes[i].data.geneid) === -1) {
                                vis.panelRef.currentQueryGeneIds.push(selectedNodes[i].data.geneid);
                            }
                        }





                        vis.panelRef.updateSearchFormGenes(vis.panelRef.currentQueryGeneIds);

                        Ext.apply(
                        vis.panelRef.coexCommand, {
                            geneIds: extendedNodesGeneIdArray,
                            queryGenesOnly: false
                        });

                        vis.panelRef.loadMask.show();
                        ExtCoexpressionSearchController.doSearchQuick2(
                        vis.panelRef.coexCommand, {
                            callback: vis.panelRef.extendThisNodeInitialCoexSearchCallback.createDelegate(vis.panelRef)

                        });

                    } else {

                        Ext.Msg.confirm('Status of Search', 'Too many Query Genes. A max of ' + Gemma.MAX_GENES_PER_CLASSIC_COEX_QUERY + ' query genes allowed. Click OK to continue search with reduced query genes', function (btn) {

                            if (btn == 'yes') {

                                //do all new searches at stringency 2 so set the spinner value
                                var spinner = vis.panelRef.getTopToolbar().getComponent('stringencySpinner');
                                spinner.setValue(2);

                                var extendedNodesGeneIdArray = [];
                                var sNodesLength = selectedNodes.length;

                                //make room in currentQueryGeneIds for new genes
                                vis.panelRef.currentQueryGeneIds = vis.panelRef.currentQueryGeneIds.splice(vis.panelRef.currentQueryGeneIds.length - (Gemma.MAX_GENES_PER_CLASSIC_COEX_QUERY - selectedNodes.length));


                                var i;
                                for (i = 0; i < sNodesLength; i++) {
                                    extendedNodesGeneIdArray[i] = selectedNodes[i].data.geneid;

                                    if (vis.panelRef.currentQueryGeneIds.indexOf(selectedNodes[i].data.geneid) === -1) {
                                        vis.panelRef.currentQueryGeneIds.push(selectedNodes[i].data.geneid);
                                    }
                                }





                                vis.panelRef.updateSearchFormGenes(vis.panelRef.currentQueryGeneIds);

                                Ext.apply(
                                vis.panelRef.coexCommand, {
                                    geneIds: extendedNodesGeneIdArray,
                                    queryGenesOnly: false
                                });

                                vis.panelRef.loadMask.show();
                                ExtCoexpressionSearchController.doSearchQuick2(
                                vis.panelRef.coexCommand, {
                                    callback: vis.panelRef.extendThisNodeInitialCoexSearchCallback.createDelegate(vis.panelRef)

                                });




                            }


                        });



                    }


                } else if (selectedNodes.length > Gemma.MAX_GENES_PER_CLASSIC_COEX_QUERY) {

                    Ext.Msg.alert('Status of Search', 'Too Many Genes Selected. Max number of selected genes is ' + Gemma.MAX_GENES_PER_CLASSIC_COEX_QUERY);

                } else {

                    Ext.Msg.alert('Status of Search', 'No Genes Selected');
                }

            });

            vis.addContextMenuItem("Re-run search with only selected nodes", "none", function (evt) {

                var selectedNodes = vis.selected("nodes");

                if (selectedNodes.length > 0 && selectedNodes.length <= Gemma.MAX_GENES_PER_CLASSIC_COEX_QUERY) {
                    //do all new searches at stringency 2 so set the spinner value
                    var spinner = vis.panelRef.getTopToolbar().getComponent('stringencySpinner');
                    spinner.setValue(2);


                    var selectedNodesGeneIdArray = [];

                    var sNodesLength = selectedNodes.length;

                    var i;
                    for (i = 0; i < sNodesLength; i++) {

                        selectedNodesGeneIdArray[i] = selectedNodes[i].data.geneid;

                    }

                    vis.panelRef.currentQueryGeneIds = selectedNodesGeneIdArray;

                    vis.panelRef.updateSearchFormGenes(selectedNodesGeneIdArray);

                    Ext.apply(
                    vis.panelRef.coexCommand, {
                        stringency: 2,
                        //Change to default or 'heuristic' based value
                        geneIds: selectedNodesGeneIdArray,
                        queryGenesOnly: false
                    });

                    vis.panelRef.loadMask.show();
                    ExtCoexpressionSearchController.doSearchQuick2(
                    vis.panelRef.coexCommand, {
                        callback: vis.panelRef.initialCoexSearchCallback.createDelegate(vis.panelRef)

                    });


                } else if (selectedNodes.length > Gemma.MAX_GENES_PER_CLASSIC_COEX_QUERY) {

                    Ext.Msg.alert('Status of Search', 'Too Many Genes Selected. Max number of selected genes is ' + Gemma.MAX_GENES_PER_CLASSIC_COEX_QUERY);

                } else {

                    Ext.Msg.alert('Status of Search', 'No Genes Selected');
                }

            });

            vis.addContextMenuItem("Re-run search with only selected nodes -my genes only-", "none", function (evt) {

                var selectedNodes = vis.selected("nodes");

                if (selectedNodes.length > 1) {

                    //do all new searches at stringency 2 so set the spinner value
                    var spinner = vis.panelRef.getTopToolbar().getComponent('stringencySpinner');
                    spinner.setValue(2);


                    var selectedNodesGeneIdArray = [];

                    var sNodesLength = selectedNodes.length;
                    var i;
                    for (i = 0; i < sNodesLength; i++) {

                        selectedNodesGeneIdArray[i] = selectedNodes[i].data.geneid;

                    }

                    vis.panelRef.currentQueryGeneIds = selectedNodesGeneIdArray;
                    vis.panelRef.updateSearchFormGenes(selectedNodesGeneIdArray);

                    Ext.apply(
                    vis.panelRef.coexCommand, {
                        stringency: 2,
                        //Change to default or 'heuristic' based value
                        geneIds: selectedNodesGeneIdArray,
                        queryGenesOnly: true
                    });

                    vis.panelRef.loadMask.show();
                    ExtCoexpressionSearchController.doSearchQuick2(
                    vis.panelRef.coexCommand, {
                        callback: vis.panelRef.initialCoexSearchCallback.createDelegate(vis.panelRef)

                    });


                } else {

                    Ext.Msg.alert('Status of Search', 'Must have at least 2 genes selected for my genes only');
                }

            });

            vis.panelRef.getTopToolbar().getComponent('stringencySpinner').value = vis.panelRef.coexCommand.stringency;


            if (!vis.panelRef.getTopToolbar().getComponent('stringencySpinner').hasListener('spin')) {

                vis.panelRef.getTopToolbar().getComponent('stringencySpinner').addListener('spin', function (ev) {

                    var spinner = vis.panelRef.getTopToolbar().getComponent('stringencySpinner');

                    if (spinner.getValue() >= vis.panelRef.currentResultsStringency) {

                        var trimmed = Gemma.CoexValueObjectUtil.trimKnownGeneResults(vis.panelRef.knownGeneResults, vis.panelRef.currentQueryGeneIds, vis.panelRef.getTopToolbar().getComponent('stringencySpinner').getValue());
                        vis.panelRef.trimmedKnownGeneResults = trimmed.trimmedKnownGeneResults;
                        vis.panelRef.trimmedNodeIds = trimmed.trimmedNodeIds;

                        vis.panelRef.coexGridRef.cytoscapeUpdate(spinner.getValue(), vis.panelRef.queryGenes.length, vis.panelRef.trimmedKnownGeneResults);


                        vis.filter("nodes", function (node) {

                            return vis.panelRef.trimmedNodeIds.indexOf(node.data.geneid) !== -1;


                        });

                        vis.filter("edges", function (edge) {

                            return edge.data.support >= vis.panelRef.getTopToolbar().getComponent('stringencySpinner').getValue();

                        });


                    } else { //new search
                        Ext.apply(
                        vis.panelRef.coexCommand, {
                            //always make 2 for now
                            //stringency: 2,
                            geneIds: vis.panelRef.currentQueryGeneIds,
                            queryGenesOnly: false
                        });

                        vis.panelRef.loadMask.show();
                        ExtCoexpressionSearchController.doSearchQuick2(
                        vis.panelRef.coexCommand, {
                            callback: vis.panelRef.initialCoexSearchCallback.createDelegate(vis.panelRef)

                        });
                    }

                }, this);


            }


            if (!vis.panelRef.getTopToolbar().getComponent('nodeDegreeEmphasis').hasListener('click')) {

                vis.panelRef.getTopToolbar().getComponent('nodeDegreeEmphasis').addListener('click', function () {

                    if (vis.panelRef.nodeDegreeVisualStyleFlag) {
                        vis.panelRef.nodeDegreeVisualStyleFlag = false;
                        vis.visualStyle(vis.panelRef.visual_style_regular);
                    } else {
                        vis.panelRef.nodeDegreeVisualStyleFlag = true;
                        vis.visualStyle(vis.panelRef.visual_style_node_degree);
                    }
                }, this);

            }


            if (!vis.panelRef.getTopToolbar().getComponent('changeLayout').hasListener('click')) {
                vis.panelRef.getTopToolbar().getComponent('changeLayout').addListener('click', function () {

                    vis.layout({
                        name: "ForceDirected"
                    });
                }, this);
            }

            vis.addContextMenuItem("Export Graph as PNG", "none", function () {

                var htmlString = '<img src="data:image/png;base64,' + vis.png() + '"/>';

                var win = new Ext.Window({
                    title: 'Graph'

                    ,
                    plain: true,
                    html: htmlString
                });
                win.show();


            });
/*
            vis.addContextMenuItem("Export Graph as graphml", "none", function () {


                var htmlString = vis.graphml();

                var win = new Ext.Window({
                    title: 'graphml',
                    height: 600,
                    width: 800,
                    plain: true,
                    html: htmlString
                });
                win.show();


            });

            vis.addContextMenuItem("Export Graph as sif", "none", function () {

                var htmlString = vis.sif();

                var win = new Ext.Window({
                    title: 'sif',
                    height: 600,
                    width: 800,
                    plain: true,
                    html: htmlString
                });
                win.show();


            });
            vis.addContextMenuItem("Export Graph as xgmml", "none", function () {

                var htmlString = vis.xgmml();

                var win = new Ext.Window({
                    title: 'xgmml',
                    height: 600,
                    width: 800,
                    plain: true,
                    html: htmlString
                });
                win.show();


            });
*/
            if (vis.panelRef.getTopToolbar().getComponent('stringencySpinner').getValue() > vis.panelRef.currentResultsStringency) {

                var trimmed = Gemma.CoexValueObjectUtil.trimKnownGeneResults(vis.panelRef.knownGeneResults, vis.panelRef.currentQueryGeneIds, vis.panelRef.getTopToolbar().getComponent('stringencySpinner').getValue());
                vis.panelRef.trimmedKnownGeneResults = trimmed.trimmedKnownGeneResults;
                vis.panelRef.trimmedNodeIds = trimmed.trimmedNodeIds;

                vis.panelRef.coexGridRef.cytoscapeUpdate(spinner.getValue(), vis.panelRef.queryGenes.length, vis.panelRef.trimmedKnownGeneResults);
                vis.panelRef.coexGridRef.knownGeneResults = vis.panelRef.knownGeneResults;
                vis.panelRef.coexGridRef.currentQueryGeneIds = vis.panelRef.currentQueryGeneIds;

                vis.filter("nodes", function (node) {

                    return vis.panelRef.trimmedNodeIds.indexOf(node.data.geneid) !== -1;


                });

                vis.filter("edges", function (edge) {

                    return edge.data.support >= vis.panelRef.getTopToolbar().getComponent('stringencySpinner').getValue();

                });
            }

            vis.panelRef.ready = true;
        });
        //end vis.ready()        
        Ext.apply(this, {

            visualization: vis



        });

        Gemma.CytoscapePanel.superclass.initComponent.apply(
        this, arguments);


    },

    ttSubstring: function (tString) {

        if (!tString) {
            return null;
        }

        var maxLength = 60;

        if (tString.length > maxLength) {
            return tString.substr(0, maxLength) + "...";
        }

        return tString;
    },
    //inputs should be two node degrees between 0 and 1, if null(missing data) return 1 as nodes/edges with 1 fade into the background
    getMaxWithNull: function (n1, n2) {

        //missing data check
        if (n1 == null || n2 == null) {
            return 1;
        }

        return Math.max(n1, n2);

    },

    decimalPlaceRounder: function (number) {

        if (number == null) {
            return null;
        }
        return Ext.util.Format.round(number, 4);

    },



    nodeDegreeBinMapper: function (nodeDegree) {

        //no data for some genes 
        if (nodeDegree == null) {
            return null;
        }
        return nodeDegree * 10;
/*
        //this should stay zero for the opacity use
        var lowVis = 0;
        if (nodeDegree > 0.9989) {
            return lowVis + 10;
        } else if (nodeDegree > 0.9899) {
            return lowVis + 9;
        } else if (nodeDegree > 0.8999) {
            return lowVis + 8;
        } else if (nodeDegree > 0.8499) {
            return lowVis + 7;
        } else if (nodeDegree > 0.7999) {
            return lowVis + 6;
        } else if (nodeDegree > 0.1999) {
            //this should be bland colour
            return lowVis + 5;
        } else if (nodeDegree > 0.1499) {
            return lowVis + 4;
        } else if (nodeDegree > 0.0999) {
            return lowVis + 4;
        } else if (nodeDegree > 0.0499) {
            return lowVis + 3;
        } else if (nodeDegree > 0.0099) {
            return lowVis + 2;
        } else if (nodeDegree > 0.0009) {
            return lowVis + 1;
        } else {
            return lowVis;
        }

*/
    },

    completeCoexSearchCallback: function (result) {

        this.queryGenes = result.queryGenes;

        this.knownGeneResults = result.knownGeneResults;
        var spinner = this.getTopToolbar().getComponent('stringencySpinner');

        //update the grid
        this.coexGridRef.cytoscapeUpdate(spinner.getValue(), this.queryGenes.length, this.knownGeneResults);
        //update underlying data 
        this.coexGridRef.knownGeneResults = this.knownGeneResults;
        this.coexGridRef.currentQueryGeneIds = this.currentQueryGeneIds;

        this.dataJSON = this.constructDataJSON(this.queryGenes, this.knownGeneResults);

        this.loadMask.hide();

        this.drawGraph();

    },

    initialCoexSearchCallback: function (result) {

        this.currentResultsStringency = this.coexCommand.stringency;

        this.currentNodeGeneIds = [];
        var qlength = result.queryGenes.length;

        //populate geneid array for complete graph
        var i;

        for (i = 0; i < qlength; i++) {


            this.currentNodeGeneIds.push(result.queryGenes[i].id);

            //this might have to go
            if (this.currentQueryGeneIds.indexOf(result.queryGenes[i].id) === -1) {
                this.currentQueryGeneIds.push(result.queryGenes[i].id);
            }

        }

        var kglength = result.knownGeneResults.length;

        for (i = 0; i < kglength; i++) {

            if (this.currentNodeGeneIds.indexOf(result.knownGeneResults[i].foundGene.id) === -1) {
                this.currentNodeGeneIds.push(result.knownGeneResults[i].foundGene.id);
            }

        }

        if (!this.coexCommand.queryGenesOnly && kglength > 0) {

            Ext.apply(
            this.coexCommand, {
                geneIds: this.currentNodeGeneIds,
                queryGenesOnly: true
            });

            ExtCoexpressionSearchController.doSearchQuick2(
            this.coexCommand, {
                callback: this.completeCoexSearchCallback.createDelegate(this)

            });
        } else {

            //store results in this for stringency filtering
            this.completeCoexSearchCallback(result);
        }


    },

    extendThisNodeInitialCoexSearchCallback: function (result) {

        var qgenes = result.queryGenes;
        var knowngenes = result.knownGeneResults;

        var kglength = knowngenes.length;

        var completeSearchFlag = false;
        var i;
        for (i = 0; i < kglength; i++) {

            if (this.currentNodeGeneIds.indexOf(knowngenes[i].foundGene.id) === -1) {
                completeSearchFlag = true;
                this.currentNodeGeneIds.push(knowngenes[i].foundGene.id);

            }
        }


        if (!completeSearchFlag) {
            Ext.Msg.alert('Status of Search', 'No more results found for this gene');
            this.loadMask.hide();
        } else {

            Ext.apply(this.coexCommand, {
                geneIds: this.currentNodeGeneIds,
                queryGenesOnly: true
            });

            ExtCoexpressionSearchController.doSearchQuick2(this.coexCommand, {
                callback: this.extendThisNodeCompleteCoexSearchCallback.createDelegate(this)

            });


        }
    },

    extendThisNodeCompleteCoexSearchCallback: function (result) {

        this.queryGenes = result.queryGenes;
        this.knownGeneResults = result.knownGeneResults;

        var spinner = this.getTopToolbar().getComponent('stringencySpinner');

        //update the grid
        this.coexGridRef.cytoscapeUpdate(spinner.getValue(), this.queryGenes.length, this.knownGeneResults);
        //update underlying data because of new results
        this.coexGridRef.knownGeneResults = this.knownGeneResults;
        this.coexGridRef.currentQueryGeneIds = this.currentQueryGeneIds;

        //update coexGrid
        this.dataJSON = this.constructDataJSON(result.queryGenes, result.knownGeneResults);

        this.loadMask.hide();

        this.drawGraph();




    },


    constructDataJSON: function (qgenes, knowngenes) {

        return this.constructDataJSONFilter(qgenes, knowngenes, false, null);

    },

    //always send in qgenes
    constructDataJSONFilter: function (qgenes, knowngenes, filterCurrentResults, filterStringency) {

        var data = {
            nodes: [],
            edges: []
        }

        //helper array to prevent duplicate nodes from being entered
        var graphNodeIds = [];

        var kglength = knowngenes.length;
        //populate node data plus populate edge data
        for (i = 0; i < kglength; i++) {

            //if not filtering go in, or if filtering: go in only if the query or known gene is contained in the original query geneids AND the stringency is >= the filter stringency
            if (!filterCurrentResults || ((this.currentQueryGeneIds.indexOf(knowngenes[i].foundGene.id) !== -1 || (this.currentQueryGeneIds.indexOf(knowngenes[i].queryGene.id) !== -1)) && (knowngenes[i].posSupp >= filterStringency || knowngenes[i].negSupp >= filterStringency))

            ) {

                if (graphNodeIds.indexOf(knowngenes[i].foundGene.id) === -1) {

                    isQueryGene = false;

                    if (this.currentQueryGeneIds.indexOf(knowngenes[i].foundGene.id) !== -1) {
                        isQueryGene = true;
                    }




                    data.nodes.push({
                        id: knowngenes[i].foundGene.officialSymbol,
                        label: knowngenes[i].foundGene.officialSymbol,
                        geneid: knowngenes[i].foundGene.id,
                        queryflag: isQueryGene,
                        officialName: this.ttSubstring(knowngenes[i].foundGene.officialName),
                        ncbiId: knowngenes[i].foundGene.ncbiId,
                        nodeDegreeBin: this.nodeDegreeBinMapper(knowngenes[i].foundGeneNodeDegree),
                        nodeDegree: this.decimalPlaceRounder(knowngenes[i].foundGeneNodeDegree)
                    });

                    graphNodeIds.push(knowngenes[i].foundGene.id);


                }

                if (graphNodeIds.indexOf(knowngenes[i].queryGene.id) === -1) {

                    isQueryGene = false;

                    if (this.currentQueryGeneIds.indexOf(knowngenes[i].queryGene.id) !== -1) {
                        isQueryGene = true;
                    }

                    data.nodes.push({
                        id: knowngenes[i].queryGene.officialSymbol,
                        label: knowngenes[i].queryGene.officialSymbol,
                        geneid: knowngenes[i].queryGene.id,
                        queryflag: isQueryGene,
                        officialName: this.ttSubstring(knowngenes[i].queryGene.officialName),
                        ncbiId: knowngenes[i].queryGene.ncbiId,
                        nodeDegreeBin: this.nodeDegreeBinMapper(knowngenes[i].queryGeneNodeDegree),
                        nodeDegree: this.decimalPlaceRounder(knowngenes[i].queryGeneNodeDegree)
                    });

                    graphNodeIds.push(knowngenes[i].queryGene.id);


                }




                var support;
                var supportsign;
                if (knowngenes[i].posSupp > 0 && knowngenes[i].negSupp > 0) {
                    support = Math.max(knowngenes[i].posSupp, knowngenes[i].negSupp);
                    supportsign = "both";

                } else if (knowngenes[i].posSupp > 0) {
                    support = knowngenes[i].posSupp;
                    supportsign = "positive";
                } else if (knowngenes[i].negSupp > 0) {
                    support = knowngenes[i].negSupp;
                    supportsign = "negative";
                }

                data.edges.push({
                    id: knowngenes[i].foundGene.officialSymbol + "to" + knowngenes[i].queryGene.officialSymbol,
                    target: knowngenes[i].foundGene.officialSymbol,
                    source: knowngenes[i].queryGene.officialSymbol,
                    positivesupport: knowngenes[i].posSupp,
                    negativesupport: knowngenes[i].negSupp,
                    support: support,
                    supportsign: supportsign,
                    nodeDegreeBin: this.nodeDegreeBinMapper(this.getMaxWithNull(knowngenes[i].queryGeneNodeDegree, knowngenes[i].foundGeneNodeDegree))
                });

            } //end if(!filterResults
        } // end for (<kglength)
        //if we are filtering, we need to loop through again to add edges that we missed the first time (because we were unsure whether both nodes would be in the graph)
        if (filterCurrentResults) {

            var completeGraphEdges = [];



            for (i = 0; i < kglength; i++) {

                //if both nodes of the edge are in the graph, and it meets the stringency threshold, and neither of the nodes are query genes(because there edges have already been added) 
                if (graphNodeIds.indexOf(knowngenes[i].foundGene.id) !== -1 && graphNodeIds.indexOf(knowngenes[i].queryGene.id) !== -1 && (knowngenes[i].posSupp >= filterStringency || knowngenes[i].negSupp >= filterStringency) && this.currentQueryGeneIds.indexOf(knowngenes[i].foundGene.id) === -1 && this.currentQueryGeneIds.indexOf(knowngenes[i].queryGene.id) === -1) {


                    var support;
                    var supportsign;
                    if (knowngenes[i].posSupp >= filterStringency && knowngenes[i].negSupp >= filterStringency) {
                        support = Math.max(knowngenes[i].posSupp, knowngenes[i].negSupp);
                        supportsign = "both";

                    } else if (knowngenes[i].posSupp >= filterStringency) {
                        support = knowngenes[i].posSupp;
                        supportsign = "positive";
                    } else if (knowngenes[i].negSupp >= filterStringency) {
                        support = knowngenes[i].negSupp;
                        supportsign = "negative";
                    }

                    data.edges.push({
                        id: knowngenes[i].foundGene.officialSymbol + "to" + knowngenes[i].queryGene.officialSymbol,
                        target: knowngenes[i].foundGene.officialSymbol,
                        source: knowngenes[i].queryGene.officialSymbol,
                        positivesupport: knowngenes[i].posSupp,
                        negativesupport: knowngenes[i].negSupp,
                        support: support,
                        supportsign: supportsign,
                        nodeDegreeBin: this.nodeDegreeBinMapper(this.getMaxWithNull(knowngenes[i].queryGeneNodeDegree, knowngenes[i].foundGeneNodeDegree))
                    });

                    completeGraphEdges.push(knowngenes[i].foundGene.officialSymbol + "to" + knowngenes[i].queryGene.officialSymbol);
                    completeGraphEdges.push(knowngenes[i].queryGene.officialSymbol + "to" + knowngenes[i].foundGene.officialSymbol);

                }


            } // end for (<kglength)
        }

        var qlength = qgenes.length;

        var isQueryGene = false;

        //add query gene nodes NOT in knowngenes, node degree set to zero
        var i;
        for (i = 0; i < qlength; i++) {

            if (graphNodeIds.indexOf(qgenes[i].id) === -1) {



                //check if this gene was part of current/previous query
                if (this.currentQueryGeneIds.indexOf(qgenes[i].id) !== -1) {
                    isQueryGene = true;
                }

                if (!filterCurrentResults || this.currentQueryGeneIds.indexOf(qgenes[i].id) !== -1) {

                    data.nodes.push({
                        id: qgenes[i].officialSymbol,
                        label: qgenes[i].officialSymbol,
                        geneid: qgenes[i].id,
                        queryflag: isQueryGene,
                        officialName: this.ttSubstring(qgenes[i].officialName),
                        ncbiId: qgenes[i].ncbiId,
                        nodeDegreeBin: 0,
                        nodeDegree: 0
                    });

                    graphNodeIds.push(qgenes[i].id);

                }

                isQueryGene = false;
            }
        }

        this.currentNodeGeneIds = graphNodeIds;

        return data;

    },

    drawGraph: function () {

        var dataMsg = {

            dataSchema: this.dataSchemaJSON,

            data: this.dataJSON

        };

        // init and draw
        this.visualization.draw({
            network: dataMsg,
            visualStyle: this.nodeDegreeVisualStyleFlag ? this.visual_style_node_degree : this.visual_style_regular,
            layout: "ForceDirected"
        });



    },

    updateSearchFormGenes: function (geneIds) {

        //clear current
        this.searchPanelRef.geneChoosers.removeAll();
        //add new genesearchandpreview
        this.searchPanelRef.addGeneChooser();
        //grab new genesearchandpreview
        var geneChooser = Ext.getCmp('geneChooser' + (this.searchPanelRef.geneChooserIndex));

        var genesToPreview = [];
        var genesToPreviewIds = [];

        var knowngenes = this.knownGeneResults;

        var kglength = knowngenes.length;
        for (i = 0; i < kglength; i++) {


            if (genesToPreviewIds.indexOf(knowngenes[i].foundGene.id) === -1 && geneIds.indexOf(knowngenes[i].foundGene.id) !== -1) {

                genesToPreview.push(knowngenes[i].foundGene);
                genesToPreviewIds.push(knowngenes[i].foundGene.id);

            }
            if (genesToPreviewIds.indexOf(knowngenes[i].queryGene.id) === -1 && geneIds.indexOf(knowngenes[i].queryGene.id) !== -1) {

                genesToPreview.push(knowngenes[i].queryGene);
                genesToPreviewIds.push(knowngenes[i].queryGene.id);

            }
        } // end for (<kglength)
        //add new genes
        geneChooser.getGenesFromCytoscape(genesToPreview, genesToPreviewIds);


    },

    coexGridUpdate: function (stringency, trimmedKnownGeneResults, trimmedNodeIds) {

        this.getTopToolbar().getComponent('stringencySpinner').setValue(stringency);

        this.trimmedKnownGeneResults = trimmedKnownGeneResults;
        this.trimmedNodeIds = trimmedNodeIds;

        filterFunctionNodes = function (node) {

            return this.trimmedNodeIds.indexOf(node.data.geneid) !== -1;


        };

        this.visualization.filter("nodes", filterFunctionNodes.createDelegate(this));


        filterFunctionEdges = function (edge) {

            return edge.data.support >= this.getTopToolbar().getComponent('stringencySpinner').getValue();

        };

        this.visualization.filter("edges", filterFunctionEdges.createDelegate(this));

    }



});
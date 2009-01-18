/**
 * User management tool.
 * 
 * @author keshav
 * @version $Id$
 */
Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

Ext.onReady(function() {
	Ext.QuickTips.init();
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

	if (Ext.isIE && !Ext.isIE7) {
		Ext.DomHelper.append('errorMessage', {
			tag : 'p',
			cls : 'trouble',
			html : 'This page may display improperly in older versions of Internet Explorer. Please upgrade to Internet Explorer 7 or newer.'
		});
	}

	// A simple test store using canned "data" and an array "reader".
	// var store = new Ext.data.Store({
	// data : [[1, "mjudge", "Judge", "foo@foo.com", true],
	// [2, "mjudge", "Judge", "foo@foo.com", true],
	// [3, "mjudge", "Judge", "foo@foo.com", true],
	// [4, "mjudge", "Judge", "foo@foo.com", false]],
	//
	// reader : new Ext.data.ArrayReader({
	// id : 'id'
	// }, ['id', 'userName', 'lastName', 'email', 'enabled'])
	//
	// });

	var record = Ext.data.Record.create([{
		name : "id",
		type : "int"
	}, {
		name : "userName",
		type : "string"
	}, {
		name : "email",
		type : "string"
	}, {
		name : "role",
		type : "string"
	}, {
		name : "enabled",
		type : "boolean"
	}]);

	var thisRecord = this.record;
	var r = record;

	var userStore = new Ext.data.Store({
		proxy : new Ext.data.DWRProxy(UserListController.getUsers),
		reader : new Ext.data.ListRangeReader({
			id : 'id'
		}, record),
		remoteSort : false
	})
	userStore.load();

	/* create editors for the grid */
	var emailEdit = new Ext.form.TextField({
		vtype : 'email'
	});

	var checkColumn = new Ext.grid.CheckColumn({
		header : "Enabled?",
		dataIndex : 'enabled',
		width : 55
	});

	// var d = userStore.data.items;
	// var r = record.get("role").createDelegate(this);

	var roleEdit = new Ext.form.ComboBox({
		typeAhead : true,
		lazyRender : true,
		triggerAction : 'all',
		mode : 'local',
		selectOnFocus : true,
		displayField : 'role',

		// the role (combo box) editor needs a store
		store : new Ext.data.Store({
			data : [[1, "user"], [2, "admin"]],

			reader : new Ext.data.ArrayReader({
				id : 'id'
			}, ['id', 'role'])

		}),

		valueField : 'role'

	});

	var userGrid = new Ext.grid.EditorGridPanel({

		renderTo : "userList",
		title : "User Management",
		frame : true,
		height : 300,
		width : 900,
		stripeRows : true,
		clicksToEdit : 1,
		plugins : checkColumn,
		loadMask : true,
		autoScroll : true,
		store : userStore,

		tbar : [

		{
			text : 'Add',
			tooltip : 'Add a new user',
			icon : 'images/icons/add.png',
			cls : 'x-btn-text-icon'
		},

		{
			text : 'Remove',
			tooltip : 'Remove selected new user',
			icon : 'images/icons/delete.png',
			cls : 'x-btn-text-icon',
			handler : function() {
				var sm = userGrid.getSelectionModel();
				var sel = sm.getSelected();
				if (sm.hasSelection()) {
					Ext.Msg.show({
						title : 'Remove User',
						buttons : Ext.MessageBox.YESNOCANCEL,
						msg : 'Remove user ' + sel.data.userName + '?',
						fn : function(btn) {
							if (btn == 'yes') {
								grid.getStore().remove(sel);
							}
						}
					});
				};
			}
		}

		],

		cm : new Ext.grid.ColumnModel([{
			header : "Username",
			dataIndex : 'userName',
			sortable : true
		}, {
			header : "Email",
			dataIndex : 'email',
			editor : emailEdit
		}, {
			header : "Role",
			dataIndex : 'role',
			editor : roleEdit
		}, checkColumn]),

		viewConfig : {
			forceFit : true
		},

		sm : new Ext.grid.RowSelectionModel({
			singleSelect : true
		})

			// listeners : {
			// afteredit : function(e) {
			// if (e.field == 'enabled' && e.value == false)
			// Ext.Msg.alert(e.record);
			// // e.record.commit();
			// }
			// }

	});
});
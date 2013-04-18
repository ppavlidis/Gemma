var uploadButton;
var uploader = new FileUpload("simpleEELoad");
var p;
var wasCancelled = false;
Ext.onReady(function() {
	// uploader.makeUploadForm("file-upload");
	var df = handleAfterUpload.createDelegate(this, [], true);
	var ff = handleFailure.createDelegate(this, [], true);
	uploadButton = new Ext.get("upload-button");
	uploader.on("finish", df);
	uploader.on("fail", ff);
	uploader.on("start", function() {
		uploadButton.disable();
	});
	uploadButton.on("click", submitForm);
});
function submitForm() {
	wasCancelled = false;
	// validate the main form, which is using struts client-side validation.
	var valid = validateSimpleEEForm(Ext.get("simpleEELoad").dom);

	// upload the file
	if (valid) {
		uploader.startUpload();
	}
}

function handleAfterUpload(data) {
	if (wasCancelled) {
		wasCancelled = false;
		return;
	}
	Ext.DomHelper.overwrite("messages", {
		tag : "img",
		src : "/Gemma/images/default/tree/loading.gif"
	});
	Ext.DomHelper.append("messages",
			"&nbsp;File upload completed, starting data processing ...");
	var fileOnServer = data.localFile;

	// This now uses the spring fields.
	var name = Ext.get("name").dom.value;
	var shortName = Ext.get("shortName").dom.value || " ";
	var description = Ext.get("description").dom.value || " ";
	var arrayDesigns = [];

	// Copy the short names into this array.
	var aropts = Ext.get("arrayDesigns").dom.options;
	var aropt;
	for (aropt in aropts) {
		if (aropts[aropt].selected) {
			arrayDesigns.push(aropts[aropt].value);
		}
	}
	var quantitationTypeName = Ext.get("quantitationTypeName").dom.value || " ";
	var quantitationTypeDescription = Ext.get("quantitationTypeDescription").dom.value
			|| " ";
	var scale = Ext.get("scale").dom.value || " ";
	var type = Ext.get("type").dom.value || " ";
	var isRatio = Ext.get("isRatio").dom.checked || false;
	var taxon = Ext.get("taxon").dom.value || Ext.get("taxonName").dom.value;
	var callParams = [];
	var commandObj = {
		dataFile : {
			localPath : fileOnServer
		},
		taxonName : taxon,
		name : name,
		shortName : shortName,
		description : description,
		arrayDesignIds : arrayDesigns,
		quantitationTypeName : quantitationTypeName,
		scale : scale,
		type : type,
		isRatio : isRatio
	};
	callParams.push(commandObj);
	var delegate = handleSuccess.createDelegate(this, [], true);
	var errorHandler = handleFailure.createDelegate(this, [], true);
	callParams.push({
		callback : delegate,
		errorHandler : errorHandler
	});
	SimpleExpressionExperimentLoadController.load.apply(this, callParams);
}
function handleCancel() {
	Ext.DomHelper.overwrite("taskId", "");
	wasCancelled = true;
}
function handleFailure(data, e) {
	reset(data);
	if (p) {
		p.stopProgress();
	}
	Ext.DomHelper.overwrite("taskId", "");
	Ext.DomHelper.overwrite("messages", {
		tag : "img",
		src : "/Gemma/images/icons/warning.png"
	});
	Ext.DomHelper.append("messages", {
		tag : "span",
		html : "&nbsp;" + data
	});
}
function reset(data) {
	// uploadButton.enable();
}
function handleSuccess(taskId) {
	try {
		Ext.DomHelper.overwrite("messages", "");
        var task = new Gemma.ObservableSubmittedTask({'taskId':taskId});
        task.on('task-failed', handleFailure);
        task.on('task-cancelling', handleCancel);
        task.showTaskProgressWindow();
	} catch (e) {
		handleFailure(taskId, e);
		return;
	}
}

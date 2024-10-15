Behaviour.specify("textarea#config\\.content", "config.content", 0, function(textarea) {
    const contentTypeElement = document.getElementsByName("_.contentType")[0];
    const readOnlyFlag = document.getElementsByName("readOnlyFlag")[0]


    if (contentTypeElement && contentTypeElement.value !== "") {
        const contentTypeVal = contentTypeElement.value;

        var editor = CodeMirror.fromTextArea(textarea, {
            lineNumbers: true,
            matchBrackets: true,
            mode: contentTypeVal,
            readOnly: !!readOnlyFlag
        });
    }
});
document.addEventListener("DOMContentLoaded", function() {
    const textarea = document.querySelector("textarea#config\\.content");
    const contentTypeElement = document.getElementsByName("_.contentType")[0];
    const readOnlyFlag = document.getElementsByName("readOnlyFlag")[0];

    if (textarea && contentTypeElement && contentTypeElement.value !== "") {
        const contentTypeVal = contentTypeElement.value;

        var editor = CodeMirror.fromTextArea(textarea, {
            lineNumbers: true,
            matchBrackets: true,
            mode: contentTypeVal,
            readOnly: !!readOnlyFlag
        });
    }
});

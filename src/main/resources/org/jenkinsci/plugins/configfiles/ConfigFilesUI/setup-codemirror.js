document.addEventListener("DOMContentLoaded", function() {
    const textarea = document.querySelector("textarea#config\\.content");
    const contentTypeElement = document.querySelector('span[data-content-type]');

    const contentTypeVal = contentTypeElement.getAttribute('data-content-type');
    const readOnly = contentTypeElement.hasAttribute('data-read-only');

    CodeMirror.fromTextArea(textarea, {
        lineNumbers: true,
        matchBrackets: true,
        mode: contentTypeVal,
        readOnly: readOnly
    });
});

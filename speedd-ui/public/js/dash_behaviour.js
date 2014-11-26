function handleDragStart(e) {
    this.style.opacity = '0.4';  // this / e.target is the source node.

    dragSrcEl = this;

    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/html', this.innerHTML);
}

function handleDragOver(e) {
    if (e.preventDefault) {
        e.preventDefault(); // Necessary. Allows us to drop.
    }

    e.dataTransfer.dropEffect = 'move';  // See the section on the DataTransfer object.

    return false;
}

function handleDragEnter(e) {
    // this / e.target is the current hover target.
    this.classList.add('over');
}

function handleDragLeave(e) {
    this.classList.remove('over');  // this / e.target is previous target element.
}

function handleDrop(e) {
    // this / e.target is current target element.

    if (e.stopPropagation) {
        e.stopPropagation(); // stops the browser from redirecting.
    }

    // See the section on the DataTransfer object.
    if (dragSrcEl != this) {
        // Set the source column's HTML to the HTML of the column we dropped on.
        dragSrcEl.innerHTML = this.innerHTML;
        this.innerHTML = e.dataTransfer.getData('text/html');
    }

    return false;
}

function handleDragEnd(e) {
    // this/e.target is the source node.
    this.style.opacity = "1";

    cols.each(function (d, i) {
        cols[0][i].classList.remove('over');
    });
}

var dragSrcEl = null;
var cols = null;
function initDrag() {
    dragSrcEl = null;
    cols = d3.selectAll('div.borders-style');
    cols.each(function (d, i) {
        cols[0][i].addEventListener('dragstart', handleDragStart, false);
        cols[0][i].addEventListener('dragenter', handleDragEnter, false);
        cols[0][i].addEventListener('dragover', handleDragOver, false);
        cols[0][i].addEventListener('dragleave', handleDragLeave, false);
        cols[0][i].addEventListener('drop', handleDrop, false);
        cols[0][i].addEventListener('dragend', handleDragEnd, false);
    });
}


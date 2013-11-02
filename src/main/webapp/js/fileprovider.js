
function fp_initDetailLink(rootURL, referenceTag){
   var selId = referenceTag.value;
   var all = new Array();
   all = document.getElementsByName('fileId');
   for(var i = 0; i < all.length; i++) {
	   if(referenceTag == all.item(i)){
		   var detailsLinkTag = document.getElementsByName('showFileDetailLink').item(i);
		   if(selId.length != 0){
			   detailsLinkTag .href=rootURL+"/configfiles/show?id=".concat(selId);
			   detailsLinkTag .style.display = 'block';
			}else{
			   detailsLinkTag .style.display = 'none';
			}
	   }
   }
}






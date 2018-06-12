$(document).ready(function(){
console.log("product list page...");
    
    getProductListByCategory(1,28,'featured');
    
  $(document).on('click','.btn.btn-light-green.btn-grid-add-to-cart',function(){
       console.log("cart has been added");
       var sku = $(this).attr('id');
       var qty='';
       $(this).parent().find(":input").each(function(i,data) { 
           var className=data.getAttribute('class');
           if(className == 'grid-product-qty-input qty-input-field'){
               qty = data.value;
           }
          
       });
       addItemToCart(sku,qty);
        
    });
    
    $(document).on('change','#itemPerPageSelect',function(){
        var recsPerPage = $(this).val();
          var currentPage = 1;
        var sortBy = $('#sortBy').val();
        getProductListByCategory(currentPage,recsPerPage,sortBy);
       
    });
    
    $(document).on('click','.pagination-next.pagination-arrow',function(e){
        e.preventDefault();
        var currentPage = parseInt($('#currentPage').val());
      
        var pageTotal = parseInt($('#totalPage').val());
        var sortBy = $('#sortBy').val();
        if(currentPage < pageTotal){
             currentPage = currentPage + 1;
            var recsPerPage = $('#itemPerPageSelect').val();
            getProductListByCategory(currentPage,recsPerPage,sortBy);
        }
       
       
    });
    
     $(document).on('click','.pagination-previous.pagination-arrow',function(e){
         e.preventDefault();
        var disabled = $(this).hasClass('disabled');
         if(!disabled){
            var currentPage = parseInt($('#currentPage').val());
             if(currentPage > 1){
             var prevPage = currentPage - 1;

                  var pageTotal = parseInt($('#totalPage').val());
                 var recsPerPage = $('#itemPerPageSelect').val();
                  var sortBy = $('#sortBy').val();
                getProductListByCategory(prevPage,recsPerPage,sortBy);

             }
         }
       
       
    });
    
    $(document).on('change','#sortBy',function(){
        var sortBy = $(this).val();
       
        var currentPage=1;
        var recsPerPage = $('#itemPerPageSelect').val();
        getProductListByCategory(currentPage,recsPerPage,sortBy);
    });
   
});

function getProductListByCategory(currentPage,recsPerPage,sortBy){
    var jsonData={};
    showLoadingScreen();
    
    
    jsonData['currentPage']=currentPage;
    jsonData['categoryId']=$('#categoryId').val();
    jsonData['noOfRecsPerPage']=recsPerPage;
   if(sortBy !== undefined && sortBy !== ''){
       if(sortBy == "featured"){
           jsonData['sortByFeatured']="DESC";
       }else if(sortBy == "newproduct" ){
           jsonData['newProduct']="DESC";
       }else{
       jsonData['sortByPrice']=sortBy;
       }
   }
    $.ajax({
        url: "/services/productlist",
        data:jsonData
    }).done(function(results){
       console.log(results); 
        hideLoadingScreen();
        var productList = JSON.parse(results);
       
        var template = $("#productlistTemplate").html();
     
        Handlebars.registerHelper("recordsPerPage",function(recsPerPage,page,totalRecs){
            var recordsPerPage = recsPerPage * page;
            if( recordsPerPage > totalRecs){
                return totalRecs;
            }else{
              return recordsPerPage;
            }
        });
        
       var html = Handlebars.compile(template);
      
        var processedHTML = html(productList)
       
       
        $('#plp-search-product-grid').empty();
        $('#plp-search-product-grid').html(processedHTML); 
        $('#itemPerPageSelect option[value='+recsPerPage +']').prop('selected',true);
         if(sortBy !== undefined && sortBy !== ''){
            $('#sortBy option[value='+ sortBy+']').prop('selected',true);
         }
        
         var $el = $('#plp-search-header-holder');
        scrollToElement($el);
        
        
        setTimeout(function() {
                   adjustHeight();
               }, 500);
        
    //enable/disable previous button - Pagination
     if(currentPage > 1){
           $('#previous').removeClass('disabled');
       }else{
           $('#previous').addClass('disabled');
       }
       
    });
}



function adjustHeight(){
    var byRow = $('#product-grid').hasClass('match-height');
   $('#product-grid').each(function() {
       $(this).children('.product-grid-item').matchHeight({
           byRow: byRow
       });
   });
}

function addItemToCart(sku,qty){
   var jsonData={};
    var cartItems={};
    var cartData={};
   showLoadingScreen();
    cartItems['sku']=sku;
    cartItems['qty']=qty;
   
    cartData['cartItem']=cartItems;
    jsonData['items']=JSON.stringify(cartData);
    jsonData['action']='add';
    
    
    $.get("/services/cart",jsonData,function(){
        
    }).done(function(result){
        console.log("result is "+result);
        hideLoadingScreen();
        if(result.trim() !== 'Error in Cart'){
        var cart = JSON.parse(result);
        $('#cartMessage-'+sku).fadeIn('fast').delay(3000).fadeOut('fast');
        var template = $("#minicartTemplate").html();
        var cartTemplate = Handlebars.compile(template);
        var html = cartTemplate(cart,cart.items.reverse());
        $('#minicarttemplate').html(html); 
        }
       
    });
}
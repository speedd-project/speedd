app.controller('CamController', ['$scope','$interval','dataService', function($scope, $interval,dataService){
   
	$scope.isCollapsed = false;
    
    $interval(changeCam, 5000);
    
    
    $scope.onClick = function (){
 //       console.log(this);
        
    }
    
    function changeCam(){
        // changes one of the small cam windows
        
        //select random cam
        var cameraToChange = dataService.randomInt(1,7);
        // select random node to view
        
        var camImage;
        var num;
        do{
            
           num = dataService.randomInt(1,19);
           camImage = "img/traffic"+num+".jpg";
           
           
           var exists = 0;
           for(var i=1; i<=7; i++)
           {
               if(camImage == dataService.cams["cam"+i])
                  exists++;           
           }
           
        }while(num==15 || exists!=0);
        
        
        d3.select("#cam"+cameraToChange).select("img").attr("src",camImage).attr("width","100%");
        
        var camera = "cam"+cameraToChange;
        dataService.cams[camera] = camImage;
    }
}]);
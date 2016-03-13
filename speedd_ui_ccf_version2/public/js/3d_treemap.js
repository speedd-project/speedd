demo = {};

    demo.Treemap3d = function() {

        "use strict";
		var w = document.getElementById("container_3d_webgl").clientWidth || document.body.clientWidth;
		var h = document.getElementById("container_3d_webgl").clientHeight || document.body.clientHeight;

        var _width          = w-40,
            _height         = h-20,
            _renderer       = null,
            _controls       = null,
            _scene          = new THREE.Scene(),
            _camera         = new THREE.PerspectiveCamera(45, _width/_height , 1, 10000),
            _zmetric        = "val",
            _zscale         = d3.scale.linear().range([0,500]),
            _elements       = null,
            _boxMap         = {},
            _showLabels     = true,
            _data           = null;
            
        var colorScale = d3.scale.category20c();

        function Treemap3d(selection) {
            _camera.setLens(30);
            _camera.position.set(100, -250, 800);
            _renderer = Modernizr.webgl ? new THREE.WebGLRenderer({antialias: true}) : new THREE.CanvasRenderer();
            _renderer.setSize(_width, _height);
            _renderer.setClearColor(0xFFFFFF);
            _renderer.domElement.style.position = 'absolute';
            _renderer.shadowMapEnabled = true;
            _renderer.shadowMapSoft = true;
            _renderer.shadowMapType = THREE.PCFShadowMap;
            _renderer.shadowMapAutoUpdate = true;

            selection.node().appendChild(_renderer.domElement);

            function enterHandler(d) {
                var boxGeometry = new THREE.BoxGeometry(1,1,1);
                var boxMaterial = new THREE.MeshLambertMaterial({color: colorScale(d["val"])});
                var box = new THREE.Mesh(boxGeometry, boxMaterial);
                box.castShadow = true;
                _boxMap[d.name] = box;
                _scene.add(box);
            }

            function updateHandler(d) {
                var duration = 1000;
                var zvalue = d[_zmetric] || 0;
                if (zvalue.toString().indexOf("$") > -1) {
                    zvalue = Number(zvalue.replace(/[^0-9\.]+/g,""));
                }
                var box = _boxMap[d.name];
                box.material.color.set(colorScale(d["val"]));
                var newMetrics = {
                    x: d.x + (d.dx / 2) - _width / 2,
                    y: d.y + (d.dy / 2) - _height / 2,
                    z: zvalue / 2,
                    w: Math.max(0, d.dx-1),
                    h: Math.max(0, d.dy-1),
                    d: zvalue
                };
                var coords = new TWEEN.Tween(box.position)
                    .to({x: newMetrics.x, y: newMetrics.y, z: newMetrics.z}, duration)
                    .easing(TWEEN.Easing.Sinusoidal.InOut)
                    .start();

                var dims = new TWEEN.Tween(box.scale)
                    .to({x: newMetrics.w, y: newMetrics.h, z: newMetrics.d}, duration)
                    .easing(TWEEN.Easing.Sinusoidal.InOut)
                    .start();

                var newRot = box.rotation;
                var rotate = new TWEEN.Tween(box.rotation)
                    .to({x: newRot.x, y: newRot.y, z: newRot.z}, duration)
                    .easing(TWEEN.Easing.Sinusoidal.InOut)
                    .start();

                var update = new TWEEN.Tween(this)
                    .to({}, duration)
                    .onUpdate(_.bind(render, this))
                    .start();

                if (_showLabels && !box.label) {
                    var label = makeTextSprite(d.name);
                    box.label = label;
                    box.label.position.setZ(1);
                    box.add(label);
                } else if (!_showLabels && box.label) {
                    box.remove(box.label);
                    delete box.label;
                }


            }

            function exitHandler(d) {
                var box = _boxMap[d.name];
                _scene.remove(box);
                delete _boxMap[d.name];
            }

            function transform() {
                TWEEN.removeAll();
                // update z-scale domain with range from selected metric
                _zscale.domain(d3.extent(_data.children, function(d) {
                    var zvalue = d[_zmetric] || 0;
                    if (zvalue.toString().indexOf("$") > -1) {
                        zvalue = Number(zvalue.replace(/[^0-9\.]+/g,""));
                    }
                    return zvalue;
                }));
                _elements.each(updateHandler);
            }

            function render() {
                _renderer.render(_scene, _camera);
            }

            function animate() {
                requestAnimationFrame(animate);
                TWEEN.update();
                _controls.update();
            }

            function makeTextSprite(message, parameters) {
                if (parameters === undefined) {
                    parameters = {};
                }
                var fontface = parameters.hasOwnProperty("fontface") ? parameters["fontface"] : "arial";
                var fontsize = parameters.hasOwnProperty("fontsize") ? parameters["fontsize"] : 35;
                var borderThickness = parameters.hasOwnProperty("borderThickness") ? parameters["borderThickness"] : 2;
                var borderColor = parameters.hasOwnProperty("borderColor") ? parameters["borderColor"] : {r: 0, g: 0, b: 0, a: 1.0};
                var backgroundColor = parameters.hasOwnProperty("backgroundColor") ? parameters["backgroundColor"] : {r: 255, g: 255, b: 255, a: 1.0};
                var textColor = parameters.hasOwnProperty("textColor") ? parameters["textColor"] : {r: 0, g: 0, b: 0, a: 1.0};
                var canvas = document.createElement('canvas');
                var context = canvas.getContext('2d');
                context.font = "Bold " + fontsize + "px " + fontface;
                var metrics = context.measureText(message);
                var textWidth = metrics.width;
                context.fillStyle = "rgba(" + backgroundColor.r + "," + backgroundColor.g + "," + backgroundColor.b + "," + backgroundColor.a + ")";
                context.strokeStyle = "rgba(" + borderColor.r + "," + borderColor.g + "," + borderColor.b + "," + borderColor.a + ")";
                context.lineWidth = borderThickness;
                roundRect(context, borderThickness / 2, borderThickness / 2, (textWidth + borderThickness) * 1.1, fontsize * 1.4 + borderThickness, 8);
                context.fillStyle = "rgba(" + textColor.r + ", " + textColor.g + ", " + textColor.b + ", 1.0)";
                context.fillText(message, borderThickness, fontsize + borderThickness);
                var texture = new THREE.Texture(canvas);
                texture.needsUpdate = true;
                var spriteMaterial = new THREE.SpriteMaterial({map: texture, useScreenCoordinates: false});
                var sprite = new THREE.Sprite(spriteMaterial);
                //sprite.scale.set(0.5 * fontsize, 0.25 * fontsize, 0.75 * fontsize);
                sprite.scale.set(5 * fontsize, 3 * fontsize, 2 * fontsize);
                return sprite;
            }

            function roundRect(ctx, x, y, w, h, r) {
                ctx.beginPath();
                ctx.moveTo(x + r, y);
                ctx.lineTo(x + w - r, y);
                ctx.quadraticCurveTo(x + w, y, x + w, y + r);
                ctx.lineTo(x + w, y + h - r);
                ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
                ctx.lineTo(x + r, y + h);
                ctx.quadraticCurveTo(x, y + h, x, y + h - r);
                ctx.lineTo(x, y + r);
                ctx.quadraticCurveTo(x, y, x + r, y);
                ctx.closePath();
                ctx.fill();
                ctx.stroke();
            }

            Treemap3d.load = function(data) {
                _data = data;
                var treemap = d3.layout.treemap()
                    .size([_width, _height])
                    .sticky(true)
                    .value(function(d) {
                        return 1;//d["amount"];
                    });

                var nodes = treemap.nodes(data);

                _elements = selection.datum(data).selectAll(".node3d")
                    .data(nodes, function(d) { return d["_id"]; });

                _elements.enter()
                    .append("div")
                    .attr("class", "node")
                    .each(enterHandler);

                _elements.each(updateHandler);

                _elements.exit().each(exitHandler).remove();

                render();
                animate();
                transform();
            };

            Treemap3d.zmetric = function(_) {
                if (!arguments.length) return _zmetric;
                _zmetric = _;
                transform();
                return this;
            };

            var directionalLight = new THREE.DirectionalLight(0xFFFFFF, 1.0);
            directionalLight.position.set(-1000, -2000, 4000);
            _scene.add(directionalLight);

            // add subtle ambient lighting
            var ambientLight = new THREE.AmbientLight(0x313131);
            _scene.add(ambientLight);

            //_controls = new THREE.OrbitControls(_camera, _renderer.domElement);
            _controls = new THREE.TrackballControls(_camera, _renderer.domElement);
            _controls.staticMoving  = true;
            _controls.minDistance = 100;
            _controls.maxDistance = 6000;
            _controls.rotateSpeed = 1.5;
            _controls.zoomSpeed = 1.5;
            _controls.panSpeed = 0.5;
            _controls.addEventListener('change', render);
        }

        return Treemap3d;
    };



///////////////////////////////////////////////////////////////
function randomInt(min, max) // function that generates a random int between min and max
{
    return Math.floor(Math.random() * (max - min + 1) + min);
}

    var filename = "data/tree_data_fraud_trans.json";
    
    var f = ["data/tree_data_fraud_trans.json","data/tree_data_fraud_trans.1.json","data/tree_data_fraud_trans.2.json","data/tree_data_fraud_trans.3.json","data/tree_data_fraud_trans.4.json","data/tree_data_fraud_trans.5.json","data/tree_data_fraud_trans.6.json"];
    
    d3.select("#explainButton").on("click", function() {

            filename = f[randomInt(0,6)];
                
            reload();
            
            console.log("click")
    });
///////////////////////////////////////////////////////////////



    d3.json(filename, function(error, data) {
        var treemap3d = demo.Treemap3d();
        d3.select("#zoptions").on("change", function() {
            treemap3d.zmetric(this.value);//.toLowerCase());
        });
        
        // function that draws the account history --- in public/js/account_history.js
       drawAccountHistory();
        ///////////////////////////////////////////////
        
        d3.select("#container_3d_webgl").append("div")
            .style("position", "absolute")
            .call(treemap3d);
        treemap3d.load(data);
        window.addEventListener("resize", function() {
            var newWidth  = window.innerWidth,
                newHeight = window.innerHeight;
            _renderer.setSize(newWidth, newHeight);
            _camera.aspect = newWidth / newHeight;
            _camera.updateProjectionMatrix();
        });
    });
    
    
    function reload(){
        
        //removes the prev plot
        d3.select("#container_3d_webgl").selectAll("canvas").remove();
        
        d3.json(filename, function(error, data) {
            var treemap3d = demo.Treemap3d();
            d3.select("#zoptions").on("change", function() {
                treemap3d.zmetric(this.value);//.toLowerCase());
            });
            
            d3.select("#container_3d_webgl").select("div")
                .style("position", "absolute")
                .call(treemap3d);
            treemap3d.load(data);
            window.addEventListener("resize", function() {
                var newWidth  = window.innerWidth,
                    newHeight = window.innerHeight;
                _renderer.setSize(newWidth, newHeight);
                _camera.aspect = newWidth / newHeight;
                _camera.updateProjectionMatrix();
            });
        });
        
  
    }
    
    /////////////////////////////////////////////////////////////////
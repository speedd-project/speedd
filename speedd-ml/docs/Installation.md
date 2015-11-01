# Instructions to build from source

## Dependencies
In order to build SPEEDD Machine Learning Module from source, you need to have Java SE Development Kit (e.g., OpenJDK) version 7 or higher and [SBT](http://www.scala-sbt.org) (v0.13.x) installed in your system. 

All library dependencies are defined inside the `build.sbt` file. The module requires the following projects to be locally build and published:

**1.** Clone and publish locally the auxlib project:
```bash
$ git clone -b v0.1 --depth 1 https://github.com/anskarl/auxlib.git
$ cd auxlib
$ sbt ++2.11.7 publishLocal
```

**2.** Clone and publish locally the Optimus project (further instructions can be found [here](https://github.com/vagm/Optimus)).
```bash
$ git clone -b v1.2.1 --depth 1 https://github.com/vagm/Optimus.git
$ cd Optimus
$ sbt publishLocal
```

**3.** Clone and publish locally the LoMRF project (further instructions can be found [here](https://github.com/anskarl/LoMRF)).

```bash
$ git clone -b v0.4.2 --depth 1 https://github.com/anskarl/LoMRF.git
$ cd LoMRF
$ sbt publishLocal
```
Once you have successfully build and published `auxlib`, `Optimus` and `LoMRF` projects, you can either build a standalone version or a "cluster" version. 


## LPSolve installation instructions 
Weight learning of SPEEDD ML requires LPSolve to be installed in your OS.

### Linux distributions 

For example, on a ***Debian-based*** distribution, write the following command:
```bash
$ sudo apt-get install lp-solve
```
  
To install Java Native Interface support for LPSolve v5.5.x you need follow the  instructions below:
* Download LPSolve dev, 64bit *lp_solve_5.5.2.x_dev_ux64.zip* or for 32bit *lp_solve_5.5.2.x_dev_ux32.zip*, from [LPSolve official repository](http://sourceforge.net/projects/lpsolve/files/lpsolve/5.5.2.0/).
  * Extract the file
  * We only need the `lpsolve55.so` file.
* Download LPSolve java bindings (lp_solve_5.5.2.x_java.zip) from [LPSolve official repository](http://sourceforge.net/projects/lpsolve/files/lpsolve/5.5.2.0/).
    * Extract the file
    * We only need the `lpsolve55j.so` files
* Create a directory containing the `lpsolve55.so` and `lpsolve55j.so` files, e.g., `$HOME/lib/lpsolve55`    
* Add this directory to `LD_LIBRARY_PATH` in your profile file:

**BASH** e.g., inside `.profile`, `.bashrc` or `.bash_profile` file in your home directory:
```bash
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$HOME/lib/lpsolve55
```

**CSH/TCSH** e.g., inside `~/.login` file in your home directory:
```csh
set LD_LIBRARY_PATH = ($LD_LIBRARY_PATH $HOME/lib/lpsolve55 .)
```
or in `~/.cshrc` file in your home directory:
```csh
setenv LD_LIBRARY_PATH $LD_LIBRARY_PATH:$HOME/lib/lpsolve55:.
```

### Apple MacOS X

Either download and install from the [LPSolve website](http://lpsolve.sourceforge.net)
or from your favorite package manager.

For example, from [macports](https://www.macports.org):
```bash
$ sudo port install lp_solve
```

or from [homebrew](http://brew.sh):
```bash
$ brew tap homebrew/science
$ brew install lp_solve
```

To install the Java Native Interface support for LPSolve v5.5.x you need follow the  instructions below:
* Download LPSolve dev, 64bit *lp_solve_5.5.2.x_dev_ux64.zip* or for 32bit *lp_solve_5.5.2.x_dev_ux32.zip*, from [LPSolve official repository](http://sourceforge.net/projects/lpsolve/files/lpsolve/5.5.2.0/).
  * Extract the file
  * We only need the `lpsolve55.dylib` file.
* Download LPSolve java bindings (lp_solve_5.5.2.x_java.zip) from [LPSolve official repository](http://sourceforge.net/projects/lpsolve/files/lpsolve/5.5.2.0/).
    * Extract the file
    * We only need the `lpsolve55j.jnilib` files
* Create a directory containing the `lpsolve55.dylib` and `lpsolve55j.jnilib` files, e.g., `$HOME/lib/lpsolve55`    
* Add this directory to `LD_LIBRARY_PATH` inside `.profile` file in your home directory:

```bash
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$HOME/lib/lpsolve55
```


### Microsoft Windows
To install LPSolve v5.5.x in your system, follow the instructions below:
  * Download LPSolve dev, 64bit *lp_solve_5.5.2.x_dev_win64.zip* or for 32bit *lp_solve_5.5.2.x_dev_win64.zip*, from [LPSolve official repository](http://sourceforge.net/projects/lpsolve/files/lpsolve/5.5.2.0/).
    * Extract the file
    * We only need the `lpsolve55.dll` file.
  * Download LPSolve java bindings (lp_solve_5.5.2.x_java.zip) from [LPSolve official repository](http://sourceforge.net/projects/lpsolve/files/lpsolve/5.5.2.0/).
    * Extract the file
    * We only need the `lpsolve55j.jar` and `lpsolve55j.dll` files
  * Create a directory containing the `lpsolve55.dll`, `lpsolve55j.jar` and `lpsolve55j.dll` files, e.g., `C:\path\to\lpsolve55`
  * Add this directory to the PATH environment variable in your system environment variables


## Build SPEEDD-ML module
To build the SPEEDD Machine Learning Module, give the following commands:
```bash
$ cd path/to/speedd/speedd-ml
$ sbt clean dist
```

After a successful compilation, the SPEEDD Machine Learning Module is located inside the `./target/universal/speedd-ml-<version>.zip` file. 
You can extract this file and add the path/to/speedd-ml-<version>/bin in your PATH, in order to execute the SPEEDD 
Machine Learning Module scripts from terminal.


## Initialize database schema

In the `schema` directory there are CQL files that define the schema for each use case. 
For example, to initialize the schema in Cassandra DB for Traffic Management use case 
write the following command:

```bash
$ cqlsh -f path/to/speedd/speedd-ml/schema/cnrs.cql
```


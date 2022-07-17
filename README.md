# NanoJ-Fluidics: open-source fluid exchange in microscopy

Note: visit the [**Wiki**][10] or [**Forum**][12] for latest updates.

![][8]

NanoJ-Fluidics is an open-source device, composed of easily accessible LEGO-parts, electronics and labware. 
It is designed to automate and simplify fluid exchange experiments in microscopy. Check the paper in Nature Communications: [Automating multimodal microscopy with
NanoJ-Fluidics][11].

## It consists of three parts:
+ [LEGO-based, multiplexable and compact syringe pumps][4]
+ [A simple "hack" to enable liquid exchange on cell culture dishes][5]
+ And a comprehensive [electronic][6] and [software][7] control suite to control the pumps.

This [Wiki][10] provides all the information necessary for researchers to reproduce their own systems
and start performing fluidic experiments on their microscopes.

## Developers
NanoJ-Fluidics is developed in a collaboration between the [Henriques][1] and [Leterrier][9] laboratories. 

## Developers
NanoJ-Fluidics is developed in a collaboration between the [Henriques][1] and [Leterrier][9] laboratories,
with contributions from the community:
  * [Matthew Meyer][0mgem0] (La Jolla Institute of Allergy & Immunology's Microscopy Core):
3D printed syringe pump body, v-slot adaptor, other parts (see [section][MayerSection]).
  * [Leo Saunders][MySaundersleo] (University of Colorado Denver): 
  * 3D printed syringe pump body (see [section][MySaundersleoSection]).

### Transition notes  
This branch currently contains code that is being in transition of working with maven.
To compile correctly, you'll need to install Micro-Manager 2.0 and,
from the NanoJ-Fluidics project directory, run the following command:
```powershell
mvn install:install-file -Dfile="PATH\plugins\Micro-Manager\MMJ_.jar" -DgroupId="org.micromanager" -DartifactId="MMJ_" -Dversion="2.0" -Dpackaging="jar"
```
Where `PATH` is your local path to where you installed micro-manager.

  [1]: http://www.ucl.ac.uk/lmcb/users/ricardo-henriques
  [2]: http://www.ucl.ac.uk/lmcb/
  [3]: http://www.ucl.ac.uk/
  [4]: https://github.com/HenriquesLab/NanoJ-Fluidics/wiki/Pumpy-Home
  [5]: https://github.com/HenriquesLab/NanoJ-Fluidics/wiki/Labware-Home
  [6]: https://github.com/HenriquesLab/NanoJ-Fluidics/wiki/Electronics-Home
  [7]: https://github.com/HenriquesLab/NanoJ-Fluidics/wiki/GUI-Home
  [8]: https://github.com/HenriquesLab/NanoJ-Fluidics/wiki/Files/PedroPumpsSample.png
  [9]: http://www.neurocytolab.org/
  [10]: https://github.com/HenriquesLab/NanoJ-Fluidics/wiki
  [11]: https://doi.org/10.1038/s41467-019-09231-9
  [12]: https://gitter.im/NanoJ-Fluidics
  [3DPrint]: Pumpy-3D-Printing
  [0mgem0]: https://twitter.com/0mgem0
  [MayerSection]: https://github.com/HenriquesLab/NanoJ-Fluidics/wiki/Pumpy-3D-Printing-MMeyer
  [MySaundersleo]: https://twitter.com/MySaundersleo
  [MySaundersleoSection]: https://github.com/HenriquesLab/NanoJ-Fluidics/wiki/Pumpy-3D-Printing-LSaunders

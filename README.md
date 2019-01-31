# **OmegaOmeroCommons**

OMEGA is designed to import BioFormats (OME Consortium, 2017) compatible image data and metadata from available image repositories. 
At the time of writing OMEGA ships with a custom-designed OMERO Image Browser, which provides a minimal interface for the user to navigate through the OMERO (Allan et al., 2012) Project, Dataset, and Image hierarchy, display available content belonging to a specific user in either a list or grid mode, select images to be analyzed and import them into OMEGA.

The functionality undergirding the OMERO Image Browser is contained in OmegaOmeroCommons, which contains all communication logic betweeen OMEGA and OMERO.Specifically, tools contained in this repository implement s and extend the OMERO.blitz library, and other components of the OMERO API to communicate with OMERO as the main source of images. 

In order to make OMEGA more interoperable, additional connectors will be developed as needed.

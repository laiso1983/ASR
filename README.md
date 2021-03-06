# ASR

An R package that performs ancestral sequence reconstruction, returns result files and generates visualisations relevant to reconstruction. The key inputs for ASR are an alignment file (Clustal or fasta) and a phylogenetic tree (Newick string). Using these inputs a reconstruction of ancestral sequences can be performed using Joint of Marginal inference. Different inference approaches provide different results. Using Joint inference will generate output containing the sequences of the reconstructed nodes. Using Marginal inference will generate output containing the probabilities of each amino acid occurring in each column of the alignment. In both cases, the original tree will be returned with all internal/ancestral nodes consistently labelled.

This ASR package will not only create the reconstruction data but also reload existing datasets (generated by this reconstruction approach) into a format that can be passed to available functions. Key functions include alignment visualisation, distribution visualisation and tree and subtree visualisation.

----------------------------------------
To use the package you only need to download and install the latest ASR_x.x.x.tar.gz file. 

The ASR folder which contains the source files should be used for development purposes. 

----------------------------------------
Download and install R 3.2.2

http://cran.us.r-project.org/ 

Mac and Windows – use precompiled binaries

Linux – use source code, untar, ./configure, make, make install, make check

----------------------------------------
Install RStudio

https://www.rstudio.com/products/rstudio/download/ 

----------------------------------------
Open RStudio and install required packages:

    install.packages(“ggplot2”)
    install.packages(“ape”)

----------------------------------------
Install ASR package

Session -> Set working directory -> Select location of ASR_x.x.x.tar.gz

    install.packages("ASR_x.x.x.tar.gz", repos = NULL, type = "source")

Common errors:

Wrong version of R - requires 3.2.2

Package dependencies (ape and ggplot2) are not installed

----------------------------------------
Load package

    library(ASR)

Load existing data

    data(asrStructure)
    
Perform ASR on new data

versions 1.0 to 1.0.4:

    asr <- runASR("tree.nwk", "aln.aln", id="runASR")

version 1.0.5:

    asr <- runASR("tree.nwk", "aln.aln")

Reload data from runASR

    asr <- loadASR("runASR")

Use loaded data (from run or load ASR)

    plot_aln(asrStructure)

Access help 

    ?plot_aln

Run examples

    examples(plot_aln)

----------------------------------------
Further examples of functions available in ASR can be found in test.R

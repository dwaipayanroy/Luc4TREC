#!/bin/sh
# To generate the sample-spec-file of the sample clueweb collection 
# NOTE: ClueWeb09_English_Sample_File.warc.gz must be present in this directory 

echo "Generating spec file for Sample ClueWeb Collection"
echo "$PWD/"ClueWeb09_English_Sample_File.warc.gz > clueweb-sample.spec
echo "Spec file generated"


#!/bin/bash
# readlink (for getting the absolute path) must be installed

# Generates the properties file and consequently execute the ClueWebIndexer program

# Generates two field Lucene index: 1. docid, 2. content.
# 1. docid: unique document-id of the document
# 2. content: clean content of each document, excluding all tags, urls, meta-info. etc.

# If you want to store the content that gets filtered during the cleaning,
#   uncomment the last portion of processDocument() in src/indexer/DocumentProcessing.java

cd ../

homepath=`eval echo ~$USER`
stopFilePath="$homepath/smart-stopwords"
if [ ! -f $stopFilePath ]
then
    echo "Please ensure that the path of the stopword-list-file is set in the .sh file."
else
    echo "Using stopFilePath="$stopFilePath
fi

### toStore
# - YES: store the raw content
# - analyzed: store the analyzed content
# - NO: do not store the content. doc.get() will return null
toStore="YES" # YES/analyzed/NO
storeTermVector="YES"
echo "Storing the content in index: "$toStore
echo "Storing the term-vectors: "$storeTermVector


if [ $# -le 1 ] 
then
    echo "Usage: " $0 " <spec-path> <index-path> [spam-score-index-path]";
    echo "1. spec-path: Path of the spec file containing the warc.gz files, one in each line";
    echo "2. index-path: Path, where the index will be created";
    echo "3. [spam-score-index-path] - optional; index-path of the Waterloo spam score";
    exit 1;
fi


prop_name="build/classes/clueweb-indexer"$#".properties"
spec_path=`readlink -f $1`		# absolute address of the .spec file

echo "Spec: "$spec_path" : "$1

if [ ! -f $spec_path ]
then
    echo "Spec file not exists"
    exit 1;
fi
index_path=`readlink -f $2`		# absolute path of where to store the index

# making the .properties file in 'build/classes'
cat > $prop_name << EOL

collSpec=$spec_path

indexPath=$index_path

toStore=$toStore

storeTermVector=$storeTermVector

stopFilePath=$stopFilePath

EOL

if [ $# -ge 3 ]
then
    if [ $# -eq 4 ]
    then
        spamScoreThreshold=$4
    fi
    cat >> $prop_name << EOL
    spamScoreIndexPath=$3

    spamScoreThreshold=$spamScoreThreshold

EOL

fi

# .properties file created in 'build/classes' 

java -cp $CLASSPATH:dist/Luc4TREC.jar:./lib/* indexer.ClueWebIndexer $prop_name

cp $prop_name $index_path/.

echo "The .properties file is saved in the index directory: "$index_path/

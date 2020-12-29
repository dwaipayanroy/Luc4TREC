#!/bin/bash
# Generate the properties file and consequently execute the indexing program

cd ../

# readlink (for getting the absolute path) must be installed

homepath=`eval echo ~$USER`
stopFilePath="$homepath/smart-stopwords"
if [ ! -f $stopFilePath ]
then
    echo "Please ensure that the path of the stopword-list-file is set in the .sh file."
    exit 1;
else
    echo "Using stopFilePath="$stopFilePath
fi

### toStore
# - YES: store the raw content
# - analyzed: store the analyzed content (NEED TO <uncomment> in DocumentProcessor.java)
# - NO: do not store the content. doc.get() will return null
toStore="YES" # YES/analyzed/NO
storeTermVector="YES"
echo "Storing the content in index: "$toStore
echo "Storing the term-vectors: "$storeTermVector

if [ $# -le 1 ] 
then
    echo "Usage: " $0 " <coll-path> <index-path> [dump-path]";
    echo "1. coll-path: ";
    echo "2. index-path: ";
    echo "3. [dump-path] - optional; specify if want to dump the index";
    exit 1;
fi

mkdir -p "prop-files" # making a directory to contain the properties file
prop_name="prop-files/msmarco-indexer.properties"
coll_path=`readlink -f $1`		# absolute address of the .properties file

if [ ! -f $coll_path ]
then
    echo "Collection file not exists"
    exit 1;
fi
index_path=`readlink -f $2`		# absolute path of where to store the index

if [ ! -d $index_path ]
then
    mkdir $index_path
fi

# making the .properties file in 'prop-files/'
cat > $prop_name << EOL

collPath=$coll_path

indexPath=$index_path

toStore=$toStore

storeTermVector=$storeTermVector

stopFilePath=$stopFilePath

EOL

if [ $# -eq 3 ]
then
    cat >> $prop_name << EOL
dumpPath=$3
EOL
fi
# .properties file created in 'build/classes' 

java -cp $CLASSPATH:dist/Luc4TREC.jar:./lib/* indexer.MSMarcoIndexer $prop_name
    
cp $prop_name $index_path/.

echo "The .properties file is saved in the index directory: "$index_path/


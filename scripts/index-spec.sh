#!/bin/bash
# Generate the properties file and consequently execute the CollectionIndex program

cd ../

# readlink (for getting the absolute path) must be installed

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
    echo "Usage: " $0 " <spec-path> <index-path> [dump-path]";
    echo "1. spec-path: ";
    echo "2. index-path: ";
    echo "3. [dump-path] - optional; specify if want to dump the index";
    exit 1;
fi

prop_name="build/classes/webdoc-indexer.properties"
spec_path=`readlink -f $1`		# absolute address of the .properties file

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

if [ $# -eq 3 ]
then
    cat >> $prop_name << EOL
dumpPath=$3
EOL
fi
# .properties file created in 'build/classes' 

java -cp $CLASSPATH:dist/Luc4TREC.jar:./lib/* indexer.NewsDocIndexer $prop_name

cp $prop_name $index_path/.

echo "The .properties file is saved in the index directory: "$index_path/


#!/bin/bash
# Generate the properties file and consequently execute the CollectionIndex program

cd ../

# readlink (for getting the absolute path) must be installed

if [[ "$USER" == "dwaipayan" ]]; then
    stopFilePath="/home/dwaipayan/smart-stopwords"
else
    homepath=`eval echo ~$USER`
    stopFilePath="$homepath/dwaipayan/smart-stopwords"
fi
# stopFilePath="/home/dwaipayan/smart-stopwords"
### toStore
# - YES: store the raw content
# - analyzed: store the analyzed content
# - NO: do not store the content. doc.get() will return null
toStore="YES" # YES/analyzed/NO
storeTermVector="NO"
toRefine="false"
bulkFileReadSize="10"
echo "Using stopFilePath="$stopFilePath
echo "Using toStore="$toStore
echo "Using storeTermVector="$storeTermVector
echo "bulkFileReadSize= "$bulkFileReadSize
echo "Refining by dropping <html-tags> and urls="$toRefine


if [ $# -le 1 ] 
then
    echo "Usage: " $0 " <tar-path> <index-path> [dump-path]";
    echo "1. tar-path: ";
    echo "2. index-path: ";
    echo "3. [dump-path] - optional; specify if want to dump the index";
    exit 1;
fi


prop_name="build/classes/tar-index.properties"
tar_path=`readlink -f $1`		# absolute address of the .properties file


echo "Continue - Press any key? (Ctrl-C to quit)"
#sleep 3     # wait for 5 second before continuing

if [ ! -f $tar_path ]
then
    echo "Tar file not exists"
    exit 1;
fi
index_path=`readlink -f $2`		# absolute path of where to store the index

# making the .properties file in 'build/classes'
cat > $prop_name << EOL

collTar=$tar_path

indexPath=$2

toStore=$toStore

storeTermVector=$storeTermVector

stopFilePath=$stopFilePath

toRefine=$toRefine

bulkFileReadSize=$bulkFileReadSize

EOL

if [ $# -eq 3 ]
then
    cat >> $prop_name << EOL
dumpPath=$3
EOL
fi
# .properties file created in 'build/classes' 

echo $prop_name

java -cp $CLASSPATH:dist/WebData.jar indexer.WebDocIndexer $prop_name

cp $prop_name $index_path/.

echo "The .properties file is saved in the index directory: "$index_path/


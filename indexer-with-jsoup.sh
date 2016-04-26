#!/bin/bash
# Generate the properties file and consequently execute the IndexWithJsoup program

# readlink (for getting the absolute path) must be installed


if [ $# -le 2 ] 
then
    echo "Usage: " $0 " <collection-name> <spec-path> <index-path> [dump-path]";
    echo "1. collection-name: The prop file will be made having this name";
    echo "2. spec-path: ";
    echo "3. index-path: ";
    echo "4. [dump-path] - optional if want to dump the index";
    exit 1;
fi


prop_name=$1".index.properties"
#echo $prop_name
spec_path=`readlink -f $2`
#echo $spec_path

if [ ! -f $spec_path ]
then
    echo "Spec file not exists"
    exit 1;
fi
index_path=`readlink -f $3`

cd build/classes

# making the .properties file
cat > $prop_name << EOL

collSpec=$spec_path

indexPath=$3

stopFile=/home/dwaipayan/smart-stopwords

EOL
if [ $# -eq 4 ]
then
    cat >> $prop_name << EOL
dumpPath=$4
EOL
fi
# .properties file made

java trecdata.IndexWithJsoup $prop_name


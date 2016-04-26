
#!/bin/bash
# Generate the properties file and consequently execute the CollectionSearcher

echo $#
if [ $# -le 4 ] 
then
    echo "Usage: " $0 " list-of-arguments";
    echo "1. collection-name: The prop file will be made having this name";
    echo "2. indexPath: Path of the index";
    echo "3. queryPath: path of the query file (in proper xml format)"
    echo "4. queryFieldFlag: 1-title, 2-title+desc, 3-title+desc+narr";
    echo "5. similarityFunction: 0.DefaultSimilarity, 1.BM25Similarity, 2.LMJelinekMercerSimilarity, 3.LMDirichletSimilarity";
    echo "6. [param1]: 'k1'-BM25; lambda-LMJM; mu-LMDi";
    echo "7. [param2]: optional 'b' if using BM25";
    exit 1;
fi

indexPath=$2
queryPath=$3
qff=$4
sf=$5

prop_name=$1"-searcher.properties"
#echo $prop_name

cd build/classes

# making the .properties file
cat > $prop_name << EOL

stopFilePath=/home/dwaipayan/smart-stopwords

indexPath=$indexPath

queryPath=$queryPath

## queryFields
# 1: title
# 2: title + desc
# 3. title + desc + narr
queryFieldFlag=$qff

### Similarity functions:
#0 - DefaultSimilarity
#1 - BM25Similarity
#2 - LMJelinekMercerSimilarity
#3 - LMDirichletSimilarity
similarityFunction=$sf

numHits=1000

EOL
if [ $# -ge 6 ]
then
    cat >> $prop_name << EOL
param1=$6
EOL
fi


if [ $# -eq 7 ]
then
    cat >> $prop_name << EOL
param2=$7
EOL
fi

java trecdata.CollectionSearcher $prop_name


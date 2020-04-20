#!/bin/bash
# Generate the properties file and consequently execute the rblm program

cd ../

# If you want to perform TRF, specify the path of qrel file in following line and make toTRF=true

homepath=`eval echo ~$USER`
stopFilePath="$homepath/smart-stopwords"
if [ ! -f $stopFilePath ]
then
    echo "Please ensure that the path of the stopword-list-file is set in the .sh file."
else
    echo "Using stopFilePath="$stopFilePath
fi

qrelPath=""
toTRF=false

if [ $# -le 5 ] 
then
    echo "Usage: " $0 " <no.-of-pseudo-rel-docs> <no.-of-expansion-terms> <query-mix (default-0.98)>";
    echo " 1. Path of the index.";
    echo " 2. Path of the query.xml file."
    echo " 3. Path of the res file."
    echo " 4. Number of expansion documents";
    echo " 5. Number of expansion terms";
    echo " 6. RM3 - QueryMix:";
    echo " 7. Field to be used for Search.   1.full-content, 2.content";
    echo " 8. Field to be used for Feedback. 1.full-content, 2.content";
    echo " 9. Similarity Function: 1:BM25, 2: LM-JM, 3: LM-Dir";
    echo "10. [Rerank]? - Yes-1  No-0 (default)"
    exit 1;
fi

indexPath=`readlink -f $1`		# absolute address of the index
queryPath=`readlink -f $2`		# absolute address of the query file
resPath=`readlink -f $3`		# absolute directory path of the .res file
resPath=$resPath"/"

echo "Using index at: "$indexPath
echo "Using query at: "$queryPath
echo "Using directory to store .res file: "$resPath

if [ $7 == "1" ]
then
    fieldToSearch="full-content"
else
    fieldToSearch="content"
fi

if [ $8 == "1" ]
then
    fieldForFeedback="full-content"
else
    fieldForFeedback="content"
fi

echo "Field for searching: "$fieldToSearch
echo "Field for feedback: "$fieldForFeedback

similarityFunction=$9

case $similarityFunction in
    1) param1=1.2
       param2=0.75 ;;
    2) param1=0.2
       param2=0.0 ;;
    3) param1=1000
       param2=0.0 ;;
    4) param1=IFB2    # dummy parameters for DFR; Config. choose is IFB2
       param2=0 ;; # dummy
esac

echo "similarity-function: "$similarityFunction" " $param1 ", "$param2


if [ $# -eq 10 ] && [ ${10} = "1" ]
then
    rerank="true"
    echo "Reranking"
else
    rerank="false"
    echo "Re-retrieving"
fi

# making the .properties file

queryName=$(basename $queryPath)
prop_name=$queryName"rm3.D-"$4".T-"$5".S-"$7".F-"$8".properties"
echo $prop_name

cat > $prop_name << EOL

indexPath=$indexPath

fieldToSearch=$fieldToSearch

fieldForFeedback=$fieldForFeedback

queryPath=$queryPath

stopFilePath=$stopFilePath

resPath=$resPath

numHits= 1000

similarityFunction=$similarityFunction

param1=$param1
param2=$param2

# Number of documents
numFeedbackDocs=$4

# Number of terms
numFeedbackTerms=$5

rm3.queryMix=$6

rm3.rerank=$rerank

qrelPath=$qrelPath

toTRF=$toTRF

feedbackFromFile=false

feedbackFilePath=$feedbackFilePath

EOL
# .properties file made

java -Xmx1g -cp $CLASSPATH:dist/Luc4TREC.jar:./lib/* feedback.RelevanceBasedLanguageModel $prop_name



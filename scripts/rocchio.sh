#!/bin/bash
# Rocchio
# Generate the properties file and consequently execute the rblm program

cd ../

# If you want to perform TRF, specify the path of qrel file in following line and make RF= (P)seudo/(T)rue

homepath=`eval echo ~$USER`
stopFilePath="$homepath/smart-stopwords"
if [ ! -f $stopFilePath ]
then
    echo "Please ensure that the path of the stopword-list-file is set in the .sh file."
# else
#     echo "Using stopFilePath="$stopFilePath
fi

qrelPath="/home/dwaipayan/Dropbox/ir/corpora-stats/qrels/all.qrel"

if [ $# -le 10 ] 
then
    echo $#
    echo "Usage: " $0 " <set of parameters>";
    echo " 1. Path of the index.";
    echo " 2. Path of the query.xml file."
    echo " 3. Path of the res file."
    echo " 4. Number of expansion documents";
    echo " 5. Number of expansion terms";
    echo " 6. Field to be used for Search";
    echo " 7. Field to be used for Feedback";
    echo " 8. ALPHA:"
    echo " 9. BETA:"
    echo "10. GAMMA:"
    echo "11. T / P: TRF or PRF"
    exit 1;
fi

indexPath=`readlink -f $1`		# absolute address of the index
queryPath=`readlink -f $2`		# absolute address of the query file
resPath=`readlink -f $3`		# absolute directory path of the .res file
resPath=$resPath"/"

# echo "Using index at: "$indexPath
# echo "Using query at: "$queryPath
# echo "Using directory to store .res file: "$resPath

fieldToSearch=$6
fieldForFeedback=$7

# echo "Field for searching: "$fieldToSearch
# echo "Field for feedback: "$fieldForFeedback

similarityFunction=1 # similarity function to be used for retrieval
# default set to BM25

case $similarityFunction in
    1) param1=1.2
       param2=0.75 ;;
    2) param1=0.2
       param2=0.0 ;;
    3) param1=1000
       param2=0.0 ;;
    4) param1=IFB2
       param2=0 ;; # dummy
esac

# echo "similarity-function: "$similarityFunction" " $param1 ", "$param2

RF=${11}

# making the .properties file

queryName=$(basename $queryPath)
prop_name=$queryName"-rocchio.D-"$4".T-"$5".S-"$7".F-"$8".properties"
echo "Using prop file at: "$prop_name

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

qrelPath=$qrelPath

RF=$RF

feedbackFromFile=false

feedbackFilePath=$feedbackFilePath

alpha=$8

beta=$9

gamma=${10}
EOL
# .properties file made

java -Xmx1g -cp $CLASSPATH:dist/Luc4TREC.jar:./lib/* feedback.CallRocchio $prop_name



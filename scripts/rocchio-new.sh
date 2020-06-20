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
else
    echo "Using stopFilePath="$stopFilePath
fi

qrelPath="/home/dwaipayan/Dropbox/ir/corpora-stats/qrels/all.qrel"
RF=T

# if [ $# -le 9 ] 
# then
#     echo $#
#     echo "Usage: " $0 " <set of parameters>";
#     echo " 1. Path of the index.";
#     echo " 2. Path of the query.xml file."
#     echo " 3. Path of the res file."
#     echo " 4. Number of expansion documents";
#     echo " 5. Number of expansion terms";
#     echo " 6. Field to be used for Search";
#     echo " 7. Field to be used for Feedback";
#     echo " 8. ALPHA:"
#     echo " 9. BETA:"
#     echo "10. GAMMA:"
#     exit 1;
# fi

while getopts "i:q:r:m:n:s:f:a:b:g:t:" OPTION; do
    case $OPTION in
        -i)
            indexPath=`readlink -f $1`
            ;;
        --index)
            shift;
            indexPath=`readlink -f $1`
            ;;
        -q)
            indexPath=`readlink -f $1`
            ;;
        --query)
            shift;
            queryPath=`readlink -f $1`
            ;;
        -r)
            resPath=`readlink -f $1`
            ;;
        --res)
            shift;
            resPath=`readlink -f $1`
            resPath=$resPath"/"
            ;;
        -m)
            numFeedbackDocs=`readlink -f $1`
            ;;
        --docs)
            shift;
            numFeedbackDocs=`readlink -f $1`
            ;;
        -n)
            numFeedbackTerms=`readlink -f $1`
            ;;
        --terms)
            shift;
            numFeedbackTerms=`readlink -f $1`
            ;;
        ## search field
        -s)
            fieldToSearch=`readlink -f $1`
            ;;
        --sfield)
            shift;
            fieldToSearch=`readlink -f $1`
            ;;
        ## feedback field
        -f)
            fieldForFeedback=`readlink -f $1`
            ;;
        --ffield)
            shift;
            fieldForFeedback=`readlink -f $1`
            ;;
        ## alpha
        -a)
            alpha=`readlink -f $1`
            ;;
        --alpha)
            shift;
            alpha=`readlink -f $1`
            ;;
        ## beta
        -b)
            beta=`readlink -f $1`
            ;;
        --beta)
            shift;
            beta=`readlink -f $1`
            ;;
        ## gamma
        -g)
            gamma=`readlink -f $1`
            ;;
        --gamma)
            shift;
            gamma=`readlink -f $1`
            ;;
        ## relevance feedback type (True / Pseudo)
        -t)
            RF=T
            qrelPath=`readlink -f $1`
            ;;
        --trf)
            shift;
            RF=T
            qrelPath=`readlink -f $1`
            ;;
        --) 
            shift
            break
        ;;
    esac
    shift
done

similarityFunction=1 # similarity function set to BM25

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

# making the .properties file

queryName=$(basename $queryPath)
prop_name=$queryName"-rocchio.D-"$4".T-"$5".S-"$7".F-"$8".properties"
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
numFeedbackDocs=$numFeedbackDocs

# Number of terms
numFeedbackTerms=$numFeedbackTerms

qrelPath=$qrelPath

RF=$RF

feedbackFromFile=false

feedbackFilePath=$feedbackFilePath

alpha=$alpha

beta=$beta

gamma=$gamma
EOL
# .properties file made

java -Xmx1g -cp $CLASSPATH:dist/Luc4TREC.jar:./lib/* feedback.CallRocchio $prop_name



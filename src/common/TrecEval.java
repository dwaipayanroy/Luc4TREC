/**
 * TrecEval: This program executes trec_eval from inside java code.
 * Dependency: trec_eval must be in $PATH.
 */
package common;

import java.io.*;

class StreamGobbler extends Thread
{
    InputStream is;
    String type;
    OutputStream os;
    
    StreamGobbler(InputStream is, String type)
    {
        this(is, type, null);
    }
    StreamGobbler(InputStream is, String type, OutputStream redirect)
    {
        this.is = is;
        this.type = type;
        this.os = redirect;
    }
    
    @Override
    public void run()
    {
        try
        {
            PrintWriter pw = null;
            if (os != null)
                pw = new PrintWriter(os);
                
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ( (line = br.readLine()) != null)
            {
                if (pw != null)
                    pw.println(line);
                System.out.println(type + "" + line);    
            }
            if (pw != null)
                pw.flush();
        } catch (IOException ioe)
            {
            ioe.printStackTrace();  
            }
    }
}

public class TrecEval
{

    /**
     * Takes the path of 1) qrel-file and 2) res-file as input, 
     * and executes trec_eval to evaluate TREC formated res file.
     * @param qrelFilePath Path to the qrel file
     * @param resFilePath Path to the res file
     */
    public static void evaluate(String qrelFilePath, String resFilePath) {

        String[] cmd = new String[3];
        cmd[0] = "trec_eval";
        cmd[1] = qrelFilePath;
        cmd[2] = resFilePath;

        try
        {
            FileOutputStream fos = new FileOutputStream(cmd[0]);
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(cmd);
            // any error message?
            StreamGobbler errorGobbler = new 
                StreamGobbler(proc.getErrorStream(), "ERROR: ");
            
            // any output?
            StreamGobbler outputGobbler = new 
                StreamGobbler(proc.getInputStream(), "", fos);
                
            // kick them off
            errorGobbler.start();
            outputGobbler.start();
                                    
            // any error???
            int exitVal = proc.waitFor();
            //System.out.println("ExitValue: " + exitVal);
            fos.flush();
            fos.close();        
        } 
        catch (IOException e) {
            System.err.println("Error: trec_eval not in executable path");
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    // for unit testing
    public static void main(String args[])
    {
        String osName = System.getProperty("os.name" );
        if( !osName.equals( "Linux" ) ){
            System.err.println("Not tested in "+osName);
        }
        if (args.length != 2)
        {
            System.out.println("USAGE java TrecEval <.qrel> <.res>");
            System.exit(1);
        }

        evaluate(args[0], args[1]);
    }
}
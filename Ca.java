import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.io.*;

public class Ca {
    static int numRes;
    static int [] availRes;
    static int numPro;
    static Process [] processes;
    static int [] safeSeq;
    
    public static void main(String []args)
    {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter number of resources: ");
        numRes = sc.nextInt();
        
        System.out.println();

        availRes = new int[numRes];
        System.out.print("Enter currently available resources for all resources: ");
        for (int i = 0; i < numRes; i++)
        {
            availRes[i] = sc.nextInt();
        }
        
        System.out.println();
        
        System.out.print("Enter number of processes: ");
        numPro = sc.nextInt();
        
        processes = new Process[numPro];
        for (int i = 0; i < numPro; i++)
        {
            System.out.print("Enter max requestable resources of each type for process " + i + ": ");
            int [] maxReqRes = new int[numRes];
            for (int j = 0; j < numRes; j++)
            {
                maxReqRes[j] = sc.nextInt();
            }
            
            System.out.print("Enter currently allocated resources of each type for process " + i + ": ");
            int [] curAlloc = new int[numRes];
            for (int j = 0; j < numRes; j++)
            {
                curAlloc[j] = sc.nextInt();
            }
            
            processes[i] = new Process(maxReqRes, curAlloc);
            System.out.println();
        }
        
        File ob = null;
        PrintWriter pw = null;
        try 
        {
            ob = new File("InitalTable.txt");
            ob.createNewFile();
            pw = new PrintWriter(ob);
        } 
        catch(IOException e) 
        {
            System.out.println("Problem in creating file, please re run the program");
            return;
        }
        
        if (!getSafeSeq(pw))
        {
            System.out.println("Not in safe state");
            for (int i = 0; i < numPro; i++)
            {
                File o = null;
                PrintWriter p = null;
                try 
                {
                    o = new File("P" + i + ".txt");
                    o.createNewFile();
                    p = new PrintWriter(o);
                } 
                catch(IOException e) 
                {
                    System.out.println("Problem in creating file, please re run the program");
                    return;
                }
                
                p.println("Intial state is not in safe state");
                p.flush();
            }
        }
        else
        {
            for (Process process: processes)
            {
                process.start();
            }
        }
        sc.close();
    }
    
    public static boolean getSafeSeq(PrintWriter pw) 
    {
        safeSeq = new int[numPro];
        Arrays.fill(safeSeq, -1);
        int tempRes[] = new int [numRes];
        for(int i = 0; i < numRes; i++)
        {
            tempRes[i] = availRes[i];
        }
        
        boolean finished[] = new boolean [numPro];
        
        int nfinished=0;
        while(nfinished < numPro) 
        {
            boolean safe = false;
            for(int i = 0; i < numPro; i++) 
            {
                if(!finished[i]) 
                {
                    boolean possible = true;
                    for(int j = 0; j < numRes; j++)
                    {
                        if(processes[i].need[j] > tempRes[j]) 
                        {
                            possible = false;
                            break;
                        }
                    }
                    
                    if(possible) {
                        pw.println("Process " + i + " can be executed as need ( " + Arrays.toString(processes[i].need) +" ) is lesser than or equal to the available ( " + Arrays.toString(tempRes) +" ) resources.");
                        String prevAvail = Arrays.toString(tempRes);
                        for(int j = 0; j < numRes; j++)
                        {
                            tempRes[j] += processes[i].curAlloc[j];
                        }
                        
                        pw.println("Available resources updated to " + Arrays.toString(tempRes) + " ( " + prevAvail + " + " + Arrays.toString(processes[i].curAlloc) + " ) after Process " + i + " completion");
                        safeSeq[nfinished] = i;
                        pw.println("Updated safe sequence is: " + Arrays.toString(safeSeq));
                        finished[i] = true;
                        ++nfinished;
                        safe = true;
                        pw.println();
                    }
                    else
                    {
                        pw.println("Process " + i + " cannot be executed (right now) as need ( " + Arrays.toString(processes[i].need) +" ) is greater than the available ( " + Arrays.toString(tempRes) +" ) resources.");
                        pw.println();
                    }
                }
            }

            if(!safe) {
                pw.println("Not in a safe state");
                for(int k = 0; k < numPro; k++) 
                {
                    safeSeq[k] = -1;
                }
                pw.flush();
                return false;
            }
        }
        
        System.out.println("It is in a safe state");
        pw.println("It is in a safe state");
        System.out.print("The safe sequence is: ");
        System.out.println(Arrays.toString(safeSeq));
        System.out.println();
        pw.flush();
        return true;
    }
}

class Process extends Thread
{
    final int pid;
    static int numProcess;
    final int [] maxReq;
    int [] curAlloc;
    int [] need;
    
    static ReentrantLock mutex = new ReentrantLock();
    
    Process (int [] maxReq, int [] curAlloc)
    {
        pid = numProcess;
        numProcess++;
        this.maxReq = maxReq;
        this.curAlloc = curAlloc;
        
        calculateNeed();
    }
    
    public void calculateNeed()
    {
        this.need = new int[curAlloc.length];
        for (int i = 0; i < need.length; i++)
        {
            need[i] = maxReq[i] - curAlloc[i];
        }
    }
    
    public void run()
    {
        mutex.lock();
        int [] reqRes = new int[maxReq.length];
        for (int i = 0; i < reqRes.length; i++)
        {
            reqRes[i] = (int) (Math.random() * (maxReq[i] - curAlloc[i] + 1));
        }
        
        System.out.println("Process " + pid + " is requesting for " + Arrays.toString(reqRes) + " resources");
        
        requestResources(reqRes);
        mutex.unlock();
    }
    
    public void requestResources(int [] reqRes)
    {
        int [] temp1 = curAlloc;
        curAlloc = new int[temp1.length];
        
        for (int i = 0; i < temp1.length; i++)
        {
            curAlloc[i] = temp1[i] + reqRes[i];
        }
        
        int [] temp2 = need;
        calculateNeed();
        
        int [] temp3 = Ca.availRes;
        Ca.availRes = new int[temp3.length];
        
        File ob = null;
        PrintWriter pw = null;
        try 
        {
            ob = new File("P" + pid + ".txt");
            ob.createNewFile();
            pw = new PrintWriter(ob);
        } 
        catch(IOException e) 
        {
            System.out.println("Problem in creating file, please re run the program");
            return;
        }
        
        for (int i = 0; i < temp3.length; i++)
        {
            if (temp3[i] < reqRes[i])
            {
                System.out.println("Resouces cannot be allocated as requested resources is greater than available resources.\n");
                pw.println("Resouces cannot be allocated as requested resources ( " + Arrays.toString(reqRes) + " ) is greater than available resources ( " + Arrays.toString(temp3) + " )");
                pw.flush();
                curAlloc = temp1;
                need = temp2;
                Ca.availRes = temp3;
                return;
            }
            Ca.availRes[i] = temp3[i] - reqRes[i];
        }
        
        pw.println("Resources " + Arrays.toString(reqRes) + " granted to check if it results in a safe state\n");
        if (!Ca.getSafeSeq(pw))
        {
            System.out.println("Resources not allocated\n");
            curAlloc = temp1;
            need = temp2;
            Ca.availRes = temp3;
        }
    }
}
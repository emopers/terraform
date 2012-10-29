package org.urbancode.terraform.tasks.rackspace;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.urbancode.terraform.tasks.common.EnvironmentTask;
import org.urbancode.terraform.tasks.common.TerraformContext;
import com.urbancode.x2o.tasks.MultiThreadTask;

public class EnvironmentTaskRackspace extends EnvironmentTask {

    //**********************************************************************************************
    // CLASS
    //**********************************************************************************************
    static private final Logger log = Logger.getLogger(EnvironmentTaskRackspace.class);
    static final private int MAX_THREADS = 30;

    //**********************************************************************************************
    // INSTANCE
    //**********************************************************************************************
    private List<ServerTask> serverTasks = new ArrayList<ServerTask>();

    //----------------------------------------------------------------------------------------------
    public EnvironmentTaskRackspace(TerraformContext context) {
        super(context);
    }

    //----------------------------------------------------------------------------------------------
    public List<ServerTask> getServerTasks(){
        return serverTasks;
    }

    //----------------------------------------------------------------------------------------------
    public ContextRackspace fetchContext() {
        return (ContextRackspace) this.context;
    }

    //----------------------------------------------------------------------------------------------
    public ServerTask createServer() {
        ServerTask server = new ServerTask(this);
        serverTasks.add(server);
        return server;
    }

    //----------------------------------------------------------------------------------------------
    /**
     * Spins off a new thread for each clone in this "chunk" to be created in parallel.
     * @param serverTaskList
     * @throws RemoteException
     * @throws InterruptedException
     * @throws Exception
     */
    private void createOrDestroyServersInParallel(List<ServerTask> serverTaskList, boolean doCreate)
    throws RemoteException, InterruptedException, Exception {
        long pollInterval = 3000L;
        long timeoutInterval = 10L * 60L * 1000L;
        long start;
        if (serverTaskList != null && !serverTaskList.isEmpty()) {
            int threadPoolSize = serverTaskList.size();
            if (threadPoolSize > MAX_THREADS) {
                threadPoolSize = MAX_THREADS;
            }

            // create instances - launch thread for each one
            List<MultiThreadTask> threadList = new ArrayList<MultiThreadTask>();
            ExecutorService service = Executors.newFixedThreadPool(threadPoolSize);
            start = System.currentTimeMillis();

            for (ServerTask instance : serverTaskList) {
                MultiThreadTask mThread = new MultiThreadTask(instance, doCreate, context);
                threadList.add(mThread);
                service.execute(mThread);
            }
            service.shutdown(); // accept no more threads

            while (!service.isTerminated()) {
                if (System.currentTimeMillis() - start > timeoutInterval) {
                    throw new RemoteException(
                            "Timeout waiting for creation Instance threads to finish");
                }
                // wait until all threads are done
                Thread.sleep(pollInterval);
            }

            // check for Exceptions caught in threads
            for (MultiThreadTask task : threadList) {
                if (task.getExceptions().size() != 0) {
                    for (Exception e : task.getExceptions()) {
                        log.error("Exception caught!", e);
                        throw e;
                    }
                }
            }
        }
        else {
            log.error("List of servers to launch was null!");
        }
    }

    //----------------------------------------------------------------------------------------------
    @Override
    public void create() {
        try {
            createOrDestroyServersInParallel(serverTasks, true);
        } catch (RemoteException e) {
            log.warn("RemoteException while creating Rackspace servers", e);
        } catch (InterruptedException e) {
            log.warn("InterruptedException while creating Rackspace servers", e);
        } catch (Exception e) {
            log.warn("Exception while creating Rackspace servers", e);
        }
    }

    //----------------------------------------------------------------------------------------------
    @Override
    public void restore() {

    }

    //----------------------------------------------------------------------------------------------
    @Override
    public void destroy() {
        try {
            createOrDestroyServersInParallel(serverTasks, false);
        } catch (RemoteException e) {
            log.warn("RemoteException while creating Rackspace servers", e);
        } catch (InterruptedException e) {
            log.warn("InterruptedException while creating Rackspace servers", e);
        } catch (Exception e) {
            log.warn("Exception while creating Rackspace servers", e);
        }
    }


}
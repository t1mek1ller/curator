package org.apache.curator.framework.imps;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorEventType;
import org.apache.curator.framework.api.CuratorListener;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.BaseClassForTests;
import org.apache.curator.test.Timing;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.WatcherType;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestRemoveWatches extends BaseClassForTests
{
    @Test
    public void testRemoveCuratorDefaultWatcher() throws Exception
    {
        Timing timing = new Timing();
        CuratorFramework client = CuratorFrameworkFactory.builder().
                connectString(server.getConnectString()).
                retryPolicy(new RetryOneTime(1)).
                build();
        try
        {
            client.start();
            
            final CountDownLatch removedLatch = new CountDownLatch(1);
            
            final String path = "/";            
            client.getCuratorListenable().addListener(new CuratorListener()
            {                
                @Override
                public void eventReceived(CuratorFramework client, CuratorEvent event)
                        throws Exception
                {
                    if(event.getType() == CuratorEventType.WATCHED && event.getWatchedEvent().getType() == EventType.DataWatchRemoved) {                        
                        removedLatch.countDown();
                    }        
                }
            });
                        
            client.checkExists().watched().forPath(path);
            
            client.watches().removeAll().forPath(path);
            
            Assert.assertTrue(timing.awaitLatch(removedLatch), "Timed out waiting for watch removal");
        }
        finally
        {
            CloseableUtils.closeQuietly(client);
        }
    }
    
    @Test
    public void testRemoveCuratorWatch() throws Exception
    {       
        Timing timing = new Timing();
        CuratorFramework client = CuratorFrameworkFactory.builder().
                connectString(server.getConnectString()).
                retryPolicy(new RetryOneTime(1)).
                build();
        try
        {
            client.start();
            
            final CountDownLatch removedLatch = new CountDownLatch(1);
            
            final String path = "/";            
            CuratorWatcher watcher = new CuratorWatcher()
            {
                
                @Override
                public void process(WatchedEvent event) throws Exception
                {
                    if(event.getPath().equals(path) && event.getType() == EventType.DataWatchRemoved) {
                        removedLatch.countDown();
                    }
                }
            };
                        
            client.checkExists().usingWatcher(watcher).forPath(path);
            
            client.watches().remove(watcher).forPath(path);
            
            Assert.assertTrue(timing.awaitLatch(removedLatch), "Timed out waiting for watch removal");
        }
        finally
        {
            CloseableUtils.closeQuietly(client);
        }
    }    
    
    @Test
    public void testRemoveWatch() throws Exception
    {       
        Timing timing = new Timing();
        CuratorFramework client = CuratorFrameworkFactory.builder().
                connectString(server.getConnectString()).
                retryPolicy(new RetryOneTime(1)).
                build();
        try
        {
            client.start();
            
            final CountDownLatch removedLatch = new CountDownLatch(1);
            
            final String path = "/";    
            Watcher watcher = new CountDownWatcher(path, removedLatch, EventType.DataWatchRemoved);
            
            client.checkExists().usingWatcher(watcher).forPath(path);
            
            client.watches().remove(watcher).forPath(path);
            
            Assert.assertTrue(timing.awaitLatch(removedLatch), "Timed out waiting for watch removal");
        }
        finally
        {
            CloseableUtils.closeQuietly(client);
        }
    }
    
    @Test
    public void testRemoveWatchInBackgroundWithCallback() throws Exception
    {       
        Timing timing = new Timing();
        CuratorFramework client = CuratorFrameworkFactory.builder().
                connectString(server.getConnectString()).
                retryPolicy(new RetryOneTime(1)).
                build();
        try
        {            
            client.start();
         
            //Make sure that the event fires on both the watcher and the callback.
            final CountDownLatch removedLatch = new CountDownLatch(2);
            final String path = "/";
            Watcher watcher = new CountDownWatcher(path, removedLatch, EventType.DataWatchRemoved);
            
            BackgroundCallback callback = new BackgroundCallback()
            {
                
                @Override
                public void processResult(CuratorFramework client, CuratorEvent event)
                        throws Exception
                {
                    if(event.getType() == CuratorEventType.REMOVE_WATCHES && event.getPath().equals(path)) {
                        removedLatch.countDown();
                    }
                }
            };
            
            
            client.checkExists().usingWatcher(watcher).forPath(path);
            
            client.watches().remove(watcher).ofType(WatcherType.Any).inBackground(callback).forPath(path);
            
            Assert.assertTrue(timing.awaitLatch(removedLatch), "Timed out waiting for watch removal");
            
        }
        finally
        {
            CloseableUtils.closeQuietly(client);
        }
    }
    
    @Test
    public void testRemoveWatchInBackgroundWithNoCallback() throws Exception
    {       
        Timing timing = new Timing();
        CuratorFramework client = CuratorFrameworkFactory.builder().
                connectString(server.getConnectString()).
                retryPolicy(new RetryOneTime(1)).
                build();
        try
        {
            client.start();
            
            final String path = "/";
            final CountDownLatch removedLatch = new CountDownLatch(1);
            Watcher watcher = new CountDownWatcher(path, removedLatch, EventType.DataWatchRemoved);
            
            client.checkExists().usingWatcher(watcher).forPath(path);
            
            client.watches().remove(watcher).inBackground().forPath(path);
            
            Assert.assertTrue(timing.awaitLatch(removedLatch), "Timed out waiting for watch removal");
            
        }
        finally
        {
            CloseableUtils.closeQuietly(client);
        }
    }        
    
    @Test
    public void testRemoveAllWatches() throws Exception
    {       
        Timing timing = new Timing();
        CuratorFramework client = CuratorFrameworkFactory.builder().
                connectString(server.getConnectString()).
                retryPolicy(new RetryOneTime(1)).
                build();
        try
        {
            client.start();
            
            final String path = "/";
            final CountDownLatch removedLatch = new CountDownLatch(2);
            
            Watcher watcher1 = new CountDownWatcher(path, removedLatch, EventType.ChildWatchRemoved);            
            Watcher watcher2 = new CountDownWatcher(path, removedLatch, EventType.DataWatchRemoved);                        
            
            client.getChildren().usingWatcher(watcher1).forPath(path);
            client.checkExists().usingWatcher(watcher2).forPath(path);
            
            client.watches().removeAll().forPath(path);
            
            Assert.assertTrue(timing.awaitLatch(removedLatch), "Timed out waiting for watch removal");
        }
        finally
        {
            CloseableUtils.closeQuietly(client);
        }
    }  
    
    @Test
    public void testRemoveAllDataWatches() throws Exception
    {       
        Timing timing = new Timing();
        CuratorFramework client = CuratorFrameworkFactory.builder().
                connectString(server.getConnectString()).
                retryPolicy(new RetryOneTime(1)).
                build();
        try
        {
            client.start();
            
            final String path = "/";
            final AtomicBoolean removedFlag = new AtomicBoolean(false);
            final CountDownLatch removedLatch = new CountDownLatch(1);
            
            Watcher watcher1 = new BooleanWatcher(path, removedFlag, EventType.ChildWatchRemoved);            
            Watcher watcher2 = new CountDownWatcher(path, removedLatch, EventType.DataWatchRemoved);                        
            
            client.getChildren().usingWatcher(watcher1).forPath(path);
            client.checkExists().usingWatcher(watcher2).forPath(path);
            
            client.watches().removeAll().ofType(WatcherType.Data).forPath(path);
            
            Assert.assertTrue(timing.awaitLatch(removedLatch), "Timed out waiting for watch removal");
            Assert.assertEquals(removedFlag.get(), false);
        }
        finally
        {
            CloseableUtils.closeQuietly(client);
        }
    }
    
    @Test
    public void testRemoveAllChildWatches() throws Exception
    {       
        Timing timing = new Timing();
        CuratorFramework client = CuratorFrameworkFactory.builder().
                connectString(server.getConnectString()).
                retryPolicy(new RetryOneTime(1)).
                build();
        try
        {
            client.start();
            
            final String path = "/";
            final AtomicBoolean removedFlag = new AtomicBoolean(false);
            final CountDownLatch removedLatch = new CountDownLatch(1);
            
            Watcher watcher1 = new BooleanWatcher(path, removedFlag, EventType.DataWatchRemoved);            
            Watcher watcher2 = new CountDownWatcher(path, removedLatch, EventType.ChildWatchRemoved);                        
                        
            client.checkExists().usingWatcher(watcher1).forPath(path);
            client.getChildren().usingWatcher(watcher2).forPath(path);
            
            client.watches().removeAll().ofType(WatcherType.Children).forPath(path);
            
            Assert.assertTrue(timing.awaitLatch(removedLatch), "Timed out waiting for watch removal");
            Assert.assertEquals(removedFlag.get(), false);
        }
        finally
        {
            CloseableUtils.closeQuietly(client);
        }
    }     
    
    /**
     * TODO: THIS IS STILL A WORK IN PROGRESS. local() is currently broken if no connection to ZK is available.
     * @throws Exception
     */
    @Test
    public void testRemoveLocalWatch() throws Exception {
        Timing timing = new Timing();
        CuratorFramework client = CuratorFrameworkFactory.builder().
                connectString(server.getConnectString()).
                retryPolicy(new RetryOneTime(1)).
                build();
        try
        {
            client.start();
            
            final String path = "/";
            
            final CountDownLatch removedLatch = new CountDownLatch(1);
            
            Watcher watcher = new CountDownWatcher(path, removedLatch, EventType.DataWatchRemoved);        
            
            client.checkExists().usingWatcher(watcher).forPath(path);
            
            //Stop the server so we can check if we can remove watches locally when offline
            server.stop();
            
            timing.sleepABit();
            
            client.watches().removeAll().locally().forPath(path);
            
            Assert.assertTrue(timing.awaitLatch(removedLatch), "Timed out waiting for watch removal");
        }
        finally
        {
            CloseableUtils.closeQuietly(client);
        }
    }
    
    /**
     * Test the case where we try and remove an unregistered watcher. In this case we expect a NoWatcherException to
     * be thrown. 
     * @throws Exception
     */
    @Test(expectedExceptions=KeeperException.NoWatcherException.class)
    public void testRemoveUnregisteredWatcher() throws Exception
    {
        CuratorFramework client = CuratorFrameworkFactory.builder().
                connectString(server.getConnectString()).
                retryPolicy(new RetryOneTime(1)).
                build();
        try
        {
            client.start();
            
            final String path = "/";            
            Watcher watcher = new Watcher() {
                @Override
                public void process(WatchedEvent event)
                {
                }                
            };
            
            client.watches().remove(watcher).forPath(path);
        }
        finally
        {
            CloseableUtils.closeQuietly(client);
        }
    }
    
    /**
     * Test the case where we try and remove an unregistered watcher but have the quietly flag set. In this case we expect success. 
     * @throws Exception
     */
    @Test
    public void testRemoveUnregisteredWatcherQuietly() throws Exception
    {
        Timing timing = new Timing();
        CuratorFramework client = CuratorFrameworkFactory.builder().
                connectString(server.getConnectString()).
                retryPolicy(new RetryOneTime(1)).
                build();
        try
        {
            client.start();
            
            final AtomicBoolean watcherRemoved = new AtomicBoolean(false);
            
            final String path = "/";            
            Watcher watcher = new BooleanWatcher(path, watcherRemoved, EventType.DataWatchRemoved);
            
            client.watches().remove(watcher).quietly().forPath(path);
            
            timing.sleepABit();
            
            //There should be no watcher removed as none were registered.
            Assert.assertEquals(watcherRemoved.get(), false);
        }
        finally
        {
            CloseableUtils.closeQuietly(client);
        }
    }
    
    private static class CountDownWatcher implements Watcher {
        private String path;
        private EventType eventType;
        private CountDownLatch removeLatch;
        
        public CountDownWatcher(String path, CountDownLatch removeLatch, EventType eventType) {
            this.path = path;
            this.eventType = eventType;
            this.removeLatch = removeLatch;            
        }
        
        @Override
        public void process(WatchedEvent event)
        {
            if(event.getPath() == null || event.getType() == null) {
                return;
            }
            
            if(event.getPath().equals(path) && event.getType() == eventType) {
                removeLatch.countDown();
            }
        }  
    }
    
    private static class BooleanWatcher implements Watcher {
        private String path;
        private EventType eventType;
        private AtomicBoolean removedFlag;
        
        public BooleanWatcher(String path, AtomicBoolean removedFlag, EventType eventType) {
            this.path = path;
            this.eventType = eventType;
            this.removedFlag = removedFlag;            
        }
        
        @Override
        public void process(WatchedEvent event)
        {
            if(event.getPath() == null || event.getType() == null) {
                return;
            }
            
            if(event.getPath().equals(path) && event.getType() == eventType) {
                removedFlag.set(true);
            }
        }  
    }    
}

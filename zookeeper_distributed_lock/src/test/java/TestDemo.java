import com.ray.Application;
import com.ray.lock.ZookeeperLock;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * @author Ray
 * @date 2022/3/31 10:47 上午
 */
@Slf4j
public class TestDemo {



    private int stock = 500;

    private final int threads = 100;

    private int i = 0;

    private int j = 0;

    private int k = 0;

    private int l = 0;

    private CountDownLatch countDownLatch = new CountDownLatch(threads);

    private CyclicBarrier cyclicBarrier = new CyclicBarrier(threads);
    @Test
    public void testZookeeperLock() throws InterruptedException {
        for (int n = 0; n < threads; n++) {
            new Thread(() -> {
                Lock zookeeperLock = new ZookeeperLock("localhost:2181", 10000, "lock", "test");
                long startTime = System.currentTimeMillis();
                try {
                    //等到全部线程准备好才开始执行，模拟并发
                    cyclicBarrier.await();
                    //尝试加锁，最多等待10秒
                    if (zookeeperLock.tryLock(10, TimeUnit.SECONDS)) {
                        if (stock > 0) {
                            stock--;
                            incrementI();
                            log.debug("剩余库存:{}", stock);
                        } else {
                            incrementJ();
                        }
                    } else {
                        incrementK();
                    }
                } catch (Exception e) {
                    incrementL();
                    log.error(e.getMessage(), e);
                } finally {
                    zookeeperLock.unlock();
                    countDownLatch.countDown();
                }
            }).start();
        }

        // 主线程休眠，不然主线程结束子线程不会执行
        countDownLatch.await();

        log.debug("已减库存 {}", i);
        log.debug("没有更多库存了 {}", j);
        log.debug("未能拿到锁 {}", k);
        log.debug("获取锁异常 {}", l);

        if (i + j + k + l == threads) {
            log.debug("成功锁住代码块");
        } else {
            log.error("未能锁住代码块");
        }
    }

    private void incrementI() {
        i++;
    }

    private void incrementJ() {
        j++;
    }

    private synchronized void incrementK() {
        k++;
    }

    private synchronized void incrementL() {
        l++;
    }
}

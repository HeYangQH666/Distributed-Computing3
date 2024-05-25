import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
//import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.awt.AWTException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Publisher {
    private static String brokerURL = "tcp://localhost:61616";//消息代理（ActiveMQ服务器）的URL
    private static ConnectionFactory factory;//创建与消息代理连接的工厂。
    private Connection connection;//管理与消息代理的连接。
    private Session session;//用于生产和消费消息的会话。
    private Topic topic;//从中发送消息的目的地；这使用了发布/订阅模型。
    private MessageProducer producer;//向主题发送消息的对象。
    private Random random = new Random();
    private static volatile boolean keepRunning = true;  // 控制运行的变量 5.9第二次加（在多线程环境中用来控制执行的标志。）
    public Publisher(String topicName) throws JMSException {
        factory = new ActiveMQConnectionFactory(brokerURL);//初始化与 ActiveMQ 的连接。
        connection = factory.createConnection();//启动连接并创建会话。
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        topic = session.createTopic(topicName); // Create a new topic 根据提供的 topicName 创建主题。
        producer = session.createProducer(topic); // Producer bound to a topic初始化一个可以向这个主题发送消息的 producer。
    }

    public void close() throws JMSException {//如果连接开启，则安全地关闭与代理的连接。
        if (connection != null) {
            connection.close();
        }
    }

    public void sendNum(double mu, double sigma) throws JMSException {//根据正态分布使用 mu（均值）和 sigma（标准差）生成一个随机数。
        double num = Math.sqrt(sigma) * random.nextGaussian() + mu;
        TextMessage message = session.createTextMessage(String.valueOf(num));//将这个数字作为 TextMessage 通过生产者发送。
        producer.send(message);
    }

    public void sendAnalysis(String ID,int num, int N, double value, double mean, double var, double min, double max) throws JMSException {
        String msg = String.format("%s %d %d %.4f %.4f %.4f %.4f %.4f", ID,num, N, value, mean, var, min, max);
        TextMessage message = session.createTextMessage(msg);
        producer.send(message);
    }////////////////// 格式化并发送一个分析结果作为 TextMessage。这个消息包括有关收集的数据点的多个统计数据。

    public static void main(String[] args) throws JMSException, AWTException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please input the number of publishers (n):");
        int n = scanner.nextInt();

        System.out.println("Please input mu and sigma:");
        double mu = scanner.nextDouble();
        double sigma = scanner.nextDouble();

        // Create a list to store multiple publishers
        List<Publisher> publishers = new ArrayList<>();

        // Initialize each publisher with its unique ID
        for (int i = 1; i <= n; i++) {
            publishers.add(new Publisher("ID" + i));
        }

//5.7注释
        //Publisher publisher1 = new Publisher("RandGaussian");//////////////////////////////
//5.9加
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(n);
//初始化 Publisher 对象并使用 ScheduledExecutorService 安排它们以固定间隔发送数据。
        //5.7注释
        /*
        for (int i = 0; i < 1000000; i++) {
            publisher1.sendNum(mu, sigma);
            robot.delay(100);
        }*/

//下面5.9换
// Execute the same sendNum operation with all publishers
     /*   for (int i = 0; i < 1000000; i++) {
            for (Publisher publisher : publishers) {
                publisher.sendNum(mu, sigma);
            }
            robot.delay(100);
        }*/

        //5.9换成了下面
        for (Publisher publisher : publishers) {
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    publisher.sendNum(mu, sigma);
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }, 0, 100, TimeUnit.MILLISECONDS);
        }

        //下面是面对生成的线为直线，缺少波动而设置的，
        /*for (Publisher publisher : publishers) {
            for (int i = 0; i < 2; i++) {//原来是1000000，现在改成20
                publisher.sendNum(mu, sigma);
                robot.delay(100);
            }
        }*/

        // Wait for user input to terminate 5.9加
       // System.out.println("Press any key to stop publishers...");
        //scanner.nextLine();

        //5.9 第二次加
        // 替换scanner.nextLine(); 使程序保持运行状态
        while (Publisher.keepRunning) {
            try {
                Thread.sleep(1000);  // 每秒检查一次是否应该停止
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Interrupted, shutting down.");
                Publisher.keepRunning = false;  // 确保外部中断可以停止程序
            }
        }//持续运行直到被中断或明确停止。
        //5.9加
        scheduler.shutdownNow();//关闭调度器并关闭所有发布者的连接。

        for (Publisher publisher : publishers) {
            publisher.close();
        }

    }
}

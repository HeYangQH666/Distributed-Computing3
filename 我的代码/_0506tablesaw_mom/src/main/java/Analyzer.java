import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import java.util.ArrayList;
//import java.util.Collections;
import java.util.Scanner;
// 消息监听类
class MyListener1 implements MessageListener {
    // 类内变量
    private int N = 0;//表示统计计算将考虑的数据点的数量。
    private int num = 0;//统计收到的消息数量
    private Double min = Double.MAX_VALUE;
    private double max = -Double.MAX_VALUE;
    private ArrayList<Double> list = new ArrayList<Double>();//存储接收到的最近 N 个数据点的列表。
    //5.7加
    private String topicID; // 添加一个成员变量存储ID，消息监听器关联的特定主题（设备）ID。

    // 构造函数，用于实例化时传入参数N，5.7改：
    // 修改构造函数，加入ID参数
    MyListener1(int N, String topicID){
        this.N = N;
        this.topicID = topicID;
    }
    // 消息接收及处理函数
    public void onMessage(Message message) {//ID
        TextMessage textmessage = (TextMessage)message;
        try {
            double value = Double.valueOf(textmessage.getText());
            list.add(value);    // 从消息队列获取一个数并加入数组
            num++;  			// 数组中的数字总数+1， 增加接收到的消息数
            //5.9修改如下：
            if (num > N) {//接收的数据点数大于 N 时，计算最近 N 个数据的均值和方差。
                double mean = 0;
                double var = 0;
                int start = list.size() - N;
                for (int i = start; i < list.size(); i++) {
                    mean += list.get(i);
                }
                mean /= N;
                for (int i = start; i < list.size(); i++) {
                    var += Math.pow((list.get(i) - mean), 2);
                }
                var /= N;
                // Update min and max more efficiently
                if (value < min) min = value;
                if (value > max) max = value;
                // Publish analysis results
                Publisher publisher2 = new Publisher("AnalysisRes");//创建 Publisher 对象并发送分析结果到主题 "AnalysisRes"。
                publisher2.sendAnalysis(topicID, num, N, value, mean, var, min, max);
                Thread.sleep(2000);
                message.acknowledge();  // 显示确认消息5.10加
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
public class Analyzer {
    public static void main(String[] args) throws JMSException {
        //5.7加
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please input the number of publishers (n):");
        int n = scanner.nextInt();
        System.out.println("Please input 统计计算应考虑的数据点数量 N:");
        int N = scanner.nextInt();

        String brokerURL = "tcp://localhost:61616";
        ConnectionFactory factory = null;
        factory = new ActiveMQConnectionFactory(brokerURL);
        Connection connection = null;
        connection = factory.createConnection();
        Session session = null;
        //session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);5.10去
        session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

        try {
            connection.start();
            for (int i = 1; i <= n; i++) {
                String topicName = "ID" + i;
                Topic topic = session.createTopic(topicName);
                MessageConsumer messageConsumer = session.createConsumer(topic);
                MyListener1 listener1 = new MyListener1(N, topicName);
                messageConsumer.setMessageListener(listener1);
                //初始化连接到消息队列，并为每个设备ID创建一个消息消费者，这些消费者使用 MyListener1 处理接收到的消息。
            }
            System.in.read();   // Pause//System.in.read() 使程序在接收到用户输入前保持运行，允许持续接收和处理消息。
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }
}
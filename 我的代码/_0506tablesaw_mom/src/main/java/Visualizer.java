
//import org.apache.activemq.ActiveMQConnectionFactory;
//import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import java.util.ArrayList;
import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import tech.tablesaw.api.Table;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.plotly.Plot;
import tech.tablesaw.plotly.api.LinePlot;
class MyListener2 implements MessageListener {
    // 类内变量
    private int count = 0;

    ArrayList<Double> list_value = new ArrayList<Double>();
    ArrayList<Double> list_avg = new ArrayList<Double>();
    ArrayList<Double> list_var = new ArrayList<Double>();
    ArrayList<Double> list_min = new ArrayList<Double>();
    ArrayList<Double> list_max = new ArrayList<Double>();
    ArrayList<String> list_name1 = new ArrayList<String>();
    ArrayList<String> list_name2 = new ArrayList<String>();
    ArrayList<String> list_name3 = new ArrayList<String>();
    ArrayList<String> list_name4 = new ArrayList<String>();
    ArrayList<String> list_name5 = new ArrayList<String>();
    ArrayList<Integer> axis = new ArrayList<Integer>();

    //声明多个列表来存储分析结果和绘图数据。

    //public MyListener2(String ID) {
    //  this.ID = ID;
    //}
    public void onMessage(Message message) {
        TextMessage textmessage = (TextMessage) message;
        try {
            String msg = String.valueOf(textmessage.getText()); // 获取信号分析结果
            String[] analysis = msg.split(" ");
            count++;                                            // 接收到的分析结果+1
            // 初始化总列表为空
            ArrayList<Integer> list_num_total = new ArrayList<Integer>();
            ArrayList<Double> list_value_total = new ArrayList<Double>();
            ArrayList<String> list_name_total = new ArrayList<String>();
            String ID = String.valueOf(analysis[0]);
            int num = Integer.valueOf(analysis[1]);                // 解包，提取各项分析结果
            int N = Integer.valueOf(analysis[2]);
            double value = Double.valueOf(analysis[3]);
            double avg = Double.valueOf(analysis[4]);
            double var = Double.valueOf(analysis[5]);
            double min = Double.valueOf(analysis[6]);
            double max = Double.valueOf(analysis[7]);
            // 实时显示信号分析结果

            System.out.println("Analysis " + ID + " " + count + " total nums：" + num + ", for the past " + N + " nums, mean: " + String.format("%.4f", avg) + ", variance: " + String.format("%.4f", var) + ", for all nums, min: " + String.format("%.4f", min) + ", max:" + String.format("%.4f", max)); //TODO add id
            // 绘制过去一段时间内的随机信号折线图，下面构造数据表的列
            axis.add(num);
            list_value.add(value);
            list_avg.add(avg);
            list_var.add(var);
            list_min.add(min);
            list_max.add(max);
            list_num_total.addAll(axis);
            list_num_total.addAll(axis);
            list_num_total.addAll(axis);
            list_num_total.addAll(axis);
            list_num_total.addAll(axis);
            list_value_total.addAll(list_value);
            list_value_total.addAll(list_avg);
            list_value_total.addAll(list_var);
            list_value_total.addAll(list_min);
            list_value_total.addAll(list_max);
            //下面为图例
            list_name1.add("高斯信号值");//list_name1.add("Gaussian Signal Value");
            list_name2.add("最近 " + N + " 个数的平均值");//list_name2.add("Avg of Last " + N + " Nums");
            list_name3.add("最近 " + N + " 个数的方差");//list_name3.add("Var of Last " + N + " Nums");
            list_name4.add("全局最小值");//list_name4.add("Global Min");
            list_name5.add("全局最大值");//list_name5.add("Global Max");
            list_name_total.addAll(list_name1);
            list_name_total.addAll(list_name2);
            list_name_total.addAll(list_name3);
            list_name_total.addAll(list_name4);
            list_name_total.addAll(list_name5);
            // 设置数据表// 创建数据表并添加中文图例
            /*Table tab = Table.create("Gaussian Signal" + ID).addColumns(
                    DoubleColumn.create("Num", list_num_total),
                    DoubleColumn.create("Value", list_value_total),
                    StringColumn.create("ValueType", list_name_total));*/
            Table tab = Table.create("高斯信号分析" + ID).addColumns(
                    DoubleColumn.create("数据点编号", list_num_total),
                    DoubleColumn.create("数值", list_value_total),
                    StringColumn.create("值类型", list_name_total));
            // 画出折线图,ValueType是分类属性
            //Plot.show(LinePlot.create("Random Signal Line Chart-" + ID, tab, "Num", "Value", "ValueType"));
            // 创建并显示图表，设置中文标题
            Plot.show(
                    LinePlot.create(
                            "随机信号线形图 - " + ID, // 中文标题
                            tab,
                            "数据点编号", // x轴标签
                            "数值", // y轴标签
                            "值类型" // 图例
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}//收消息并解析数据，更新图表数据并动态显示
public class Visualizer {
    public static void main(String[] args) throws JMSException {
        String brokerURL = "tcp://localhost:61616";
        ConnectionFactory factory = null;
        Connection connection = null;
        Session session = null;
        Topic topic = null;
        MessageConsumer messageConsumer = null;
        MyListener2 listener2 = null;
        try {
            factory = new ActiveMQConnectionFactory(brokerURL);
            connection = factory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            // 订阅信号分析结果
            topic = session.createTopic("AnalysisRes");////
            messageConsumer = session.createConsumer(topic);
            listener2 = new MyListener2();//Todo 2024/5/8
            messageConsumer.setMessageListener(listener2);//注册 MyListener2 作为消息消费者，并接收 "AnalysisRes" 主题的消息。
            connection.start();
            System.out.println("Press any key to exit the visualizer..");//程序在接收到输入之前会持续运行，等待消息并进行处理。
            System.in.read();   // Pause
        } catch (Exception e) {
            e.printStackTrace();
        } finally {//程序在接收到输入之前会持续运行，等待消息并进行处理。
            if (connection != null) {
                connection.close();
            }
        }
    }
}
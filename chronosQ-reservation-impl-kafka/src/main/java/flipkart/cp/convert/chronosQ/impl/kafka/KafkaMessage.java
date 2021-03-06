package flipkart.cp.convert.chronosQ.impl.kafka;


import flipkart.cp.convert.chronosQ.core.SchedulerEntry;
import org.apache.kafka.clients.producer.ProducerRecord;

public interface KafkaMessage {
    ProducerRecord<byte[], byte[]> getKeyedMessage(String topic, SchedulerEntry schedulerEntry);
}

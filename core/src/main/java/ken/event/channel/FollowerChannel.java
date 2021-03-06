package ken.event.channel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import ken.event.Event;
import ken.event.client.follower.AdvancedFollower;
import ken.event.client.follower.IFollower;

import org.apache.log4j.Logger;

import backtype.storm.Config;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

/**
 * @author KennyZJ
 * 
 */
public class FollowerChannel extends BaseRichSpout {

	private static final long serialVersionUID = 1L;

	public static Logger LOG = Logger.getLogger(FollowerChannel.class);

	@SuppressWarnings("rawtypes")
	private LinkedBlockingQueue<Event> _queue;
	private String _key;
	boolean _isDistributed;
	SpoutOutputCollector _collector;
	private IFollower ifo;

	public FollowerChannel(String key) {
		this(true);
		_key = key;
	}

	public FollowerChannel(boolean isDistributed) {
		_isDistributed = isDistributed;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void open(Map conf, TopologyContext context,
			SpoutOutputCollector collector) {
		_collector = collector;
		String fid = String.format("%s-%s-%d", context.getStormId(),
				context.getThisComponentId(), context.getThisTaskId());
		try {
			ifo = new AdvancedFollower(fid, _key, _queue);
//			ifo = new AdvancedFollower(fid, _key);
			ifo.startFollowing();
		} catch (IOException e) {
			LOG.error(e);
		} catch (Exception e) {
			LOG.error(e);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void nextTuple() {
		try {
			Event evt = _queue.take(); // blocking way

			_collector.emit(new Values(evt));
		} catch (InterruptedException e) {
			LOG.error(e);
		}
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("eventdata"));
	}

	@Override
	public void ack(Object msgId) {
		// TODO HA effort
	}

	@Override
	public void fail(Object msgId) {
		// TODO HA effort
	}

	@Override
	public void close() {
		if (ifo != null) {
			ifo.stopFollowing();
			ifo = null;
		}
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			LOG.error(e);
		}
	}

	@Override
	public Map<String, Object> getComponentConfiguration() {
		Map<String, Object> ret = new HashMap<String, Object>();
		if (!_isDistributed) {
			ret.put(Config.TOPOLOGY_MAX_TASK_PARALLELISM, 1);
		}
		return ret;
	}
}

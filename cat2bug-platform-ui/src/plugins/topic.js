import 'pubsub-js'
class topic {
  static MEMBER_FOCUS_TOPIC = "memberFocus";
  static MEMBER_ONLINE_TOPIC = "memberOnline";
  static MEMBER_OFFLINE_TOPIC = "memberOffline";
  static AI_MODEL_PULL_TOPIC = "aiModelPull";
  static NOTICE_TOPIC = "notice";
  /** 推送数据 */
  static publish(topic, data) {
    window.PubSub.publish(topic, data);
  }
  /** 订阅数据 */
  static subscribe(topic, callback) {
    window.PubSub.subscribe(topic, callback);
  }
  /** 取消订阅数据 */
  static unsubscribe(id) {
    window.PubSub.unsubscribe(id);
  }
}
export default topic;

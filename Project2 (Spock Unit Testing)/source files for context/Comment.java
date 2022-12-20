package spock.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Entity
@Table(name = "comment")
public class Comment implements Comparable<Comment> {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "comment_id", length = 20)
	private Integer commentId;

	@ManyToOne
	private SpockUser user;

	@Column(name = "user_name", length = 101)
	private String userName;    //convenience attribute

	@ManyToOne
	@Cascade(CascadeType.SAVE_UPDATE)
	private Lecture lecture;

	@OneToOne
	private Icon icon;

	@Column(name = "text", length = 500)
	private String text;

	@Column(name = "post_timestamp", length = 30)
	private String postTimestamp;

	@Column(name = "video_timestamp", length = 30)
	private String videoTimestamp;

	@Column(name = "votes_up", length = 6)
	private Integer votesUp;

	@Column(name = "votes_down", length = 6)
	private Integer votesDown;

	@Column(name = "comment_reply", length = 1)
	private Boolean emailOnCommentReply;

	@Column(name = "anonymous", length = 1)
	private Boolean anonymous;

	@Column(name = "instructorOrSuperUser", length = 1)
	private Boolean instructorOrSuperUser;

	@OneToMany
	@Cascade(CascadeType.DELETE)
	@LazyCollection(LazyCollectionOption.FALSE)
	private List<Reply> replies;

	public Comment() {
		this.replies = new ArrayList<Reply>();
	}

	public Comment(SpockUser user, Lecture lecture, Icon icon, String text, String postTimestamp, String videoTimestamp, Boolean emailOnCommentReply, Boolean isAnonymous) {
		this();
		this.user = user;
		//temporary
		if (isAnonymous) {
			this.userName = "";
		} else {
			this.userName = user.getFirstname() + " " + user.getLastname();
		}
		this.lecture = lecture;
		this.icon = icon;
		this.text = text;
		this.postTimestamp = postTimestamp;
		this.videoTimestamp = videoTimestamp;
		this.votesUp = 0;
		this.votesDown = 0;
		this.emailOnCommentReply = emailOnCommentReply;
		this.anonymous = isAnonymous;

		if(user.isInstructorOrSuperUser()){
			this.instructorOrSuperUser = true;
		}else{
			this.instructorOrSuperUser = false;
		}
	}

	public boolean getIsInstructorOrSuperUser(){
		return this.instructorOrSuperUser;
	}

	public void setIsInstructorOrSuperUser (Boolean inst){
		this.instructorOrSuperUser = inst;
	}

	public Integer getCommentId() {
		return commentId;
	}

	public void setCommentId(Integer commentId) {
		this.commentId = commentId;
	}

	@JsonIgnore
	public SpockUser getUser() {
		return user;
	}

	public void setUser(SpockUser user) {
		this.user = user;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	@JsonIgnore
	public Lecture getLecture() {
		return lecture;
	}

	public void setLecture(Lecture lecture) {
		this.lecture = lecture;
	}

	public Icon getIcon() {
		return icon;
	}

	public void setIcon(Icon icon) {
		this.icon = icon;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getPostTimestamp() {
		return postTimestamp;
	}

	public void setPostTimestamp(String postTimestamp) {
		this.postTimestamp = postTimestamp;
	}

	public String getVideoTimestamp() {
		return videoTimestamp;
	}

	public void setVideoTimestamp(String videoTimestamp) {
		this.videoTimestamp = videoTimestamp;
	}

	public Integer getVotesUp() {
		return votesUp;
	}

	public void setVotesUp(Integer votesUp) {
		this.votesUp = votesUp;
	}

	public void incrementVotesUp() {
		this.votesUp++;
	}

	public Integer getVotesDown() {
		return votesDown;
	}

	public void setVotesDown(Integer votesDown) {
		this.votesDown = votesDown;
	}

	public void incrementVotesDown() {
		this.votesDown++;
	}

	public Boolean getEmailOnCommentReply() {
		return emailOnCommentReply;
	}

	public void setEmailOnCommentReply(Boolean emailOnCommentReply) {
		this.emailOnCommentReply = emailOnCommentReply;
	}

	public Boolean getAnonymous() {
		return anonymous;
	}

	public void setAnonymous(Boolean anonymous) {
		this.anonymous = anonymous;
	}

	public List<Reply> getReplies() {
		Collections.sort(this.replies);
		return this.replies;
	}

	public void setReplies(List<Reply> replies) {
		this.replies = replies;
	}

	public void addReply(Reply reply) {
		this.replies.add(reply);
	}

	public int getVoteRating() {
		return this.votesUp - this.votesDown;
	}

	@Override
	public int compareTo(Comment o) {
		if (this.lecture != null) {
			int lectureCompare = this.lecture.compareTo(o.lecture);
			if (lectureCompare != 0) {
				return lectureCompare;
			}
		}
		if (this.videoTimestamp != null) {
			int videoTimestampCompare = this.videoTimestamp.compareTo(o.videoTimestamp);
			if (videoTimestampCompare != 0) {
				return videoTimestampCompare;
			}
		}
		return this.postTimestamp.compareTo(o.postTimestamp);
	}

	@Override
	public String toString() {
		return "Comment [commentId=" + commentId + ", text=" + text + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((commentId == null) ? 0 : commentId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Comment other = (Comment) obj;
		if (commentId == null) {
			if (other.commentId != null)
				return false;
		} else if (!commentId.equals(other.commentId))
			return false;
		return true;
	}

}
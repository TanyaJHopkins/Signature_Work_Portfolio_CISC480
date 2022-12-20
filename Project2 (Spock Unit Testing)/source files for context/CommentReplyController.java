package spock.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import spock.dao.CommentReplyDao;
import spock.dao.CourseUnitLectureDao;
import spock.dao.NotificationDao;
import spock.dao.SpockUserDao;
import spock.model.*;
import spock.util.SpockUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Set;

@Controller
public class CommentReplyController {

	@Autowired
	private SimpMessagingTemplate template;

	@ResponseBody
	@RequestMapping(value="/comment", method = RequestMethod.POST, produces="application/json")
	public String addComment(HttpServletRequest request, @RequestParam("lectureId") String lectureIdStr, @RequestParam("text") String text, @RequestParam("videoTimestamp") String videoTimestamp, @RequestParam(value = "emailOnCommentReply", required = false) boolean emailOnCommentReply, @RequestParam(value="isAnonymous", required = false) boolean isAnonymous) {
		String result = "";
		//check authentication
		if(!SpockUtil.isUserAuthenticated(request)) {
			return result;
		}
		SpockUser sessionUser = SpockUtil.getUserFromSession(request);
		ObjectMapper mapper = new ObjectMapper();
		try {
			Integer lectureId = Integer.parseInt(lectureIdStr);
			Lecture lecture = CourseUnitLectureDao.getLecture(lectureId);
			for(SpockUserCourse userCourse : sessionUser.getUserCourses()) {
				//user has access to course
				Course course = userCourse.getCourse();
				if(course.equals(lecture.getUnit().getCourse())) {
					String postTimestamp = Integer.toString((int) (System.currentTimeMillis() / 1000L));
					Icon icon = CommentReplyDao.getStudentIcon(sessionUser, lecture, request.getServletContext().getRealPath("/"));
					Comment comment = new Comment(sessionUser, lecture, icon, SpockUtil.sanitizeText(text), postTimestamp, videoTimestamp, emailOnCommentReply, isAnonymous);
					CommentReplyDao.addComment(comment);
					return mapper.writeValueAsString(comment);
				}
			}
		} catch(NumberFormatException nfe) {
			nfe.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

		return result;
	}

	@ResponseBody
	@RequestMapping(value="/reply", method = RequestMethod.POST, produces = "application/json")
	public String addReply(HttpServletRequest request, @RequestParam("lectureId") String lectureId, @RequestParam("commentId") String commentIdStr, @RequestParam("text") String text, @RequestParam(value = "emailOnReplyReply", required = false) boolean emailOnReplyReply, @RequestParam(value="isAnonymous", required = false) boolean isAnonymous) {
		String result = "";
		//check authentication
		if(!SpockUtil.isUserAuthenticated(request)) {
			return result;
		}
		SpockUser sessionUser = SpockUtil.getUserFromSession(request);
		ObjectMapper mapper = new ObjectMapper();
		try {
			Integer commentId = Integer.parseInt(commentIdStr);
			Comment comment = CommentReplyDao.getComment(commentId);
			Lecture lecture = comment.getLecture();
			for(SpockUserCourse userCourse : sessionUser.getUserCourses()) {
				//user has access to course
				Course course = userCourse.getCourse();
				if(course.equals(lecture.getUnit().getCourse())) {
					String postTimestamp = Integer.toString((int) (System.currentTimeMillis() / 1000L));
					Icon icon = CommentReplyDao.getStudentIcon(sessionUser, lecture, request.getServletContext().getRealPath("/"));
					Reply reply = new Reply(sessionUser, comment.getCommentId(), icon, SpockUtil.sanitizeText(text), postTimestamp, emailOnReplyReply, isAnonymous);
					CommentReplyDao.addReply(reply);
					comment.addReply(reply);
					CommentReplyDao.updateComment(comment);
					return mapper.writeValueAsString(reply);
				}
			}
		} catch(NumberFormatException nfe) {
			nfe.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

		return result;
	}

	@ResponseBody
	@RequestMapping(value="/voteUpComment", method = RequestMethod.POST, produces = "application/json")
	public String voteUpComment(HttpServletRequest request, @RequestParam("courseId") String courseIdStr, @RequestParam("commentId") String commentIdStr) {
		String result = "";
		//check authentication
		if(!SpockUtil.isUserAuthenticated(request)) {
			return result;
		}
		SpockUser sessionUser = SpockUtil.getUserFromSession(request);
		try {
			Integer courseId = Integer.parseInt(courseIdStr);
			Integer commentId = Integer.parseInt(commentIdStr);
			Comment comment = CommentReplyDao.getComment(commentId);
			if(sessionUser.hasCourseAccess(courseId) && !sessionUser.equals(comment.getUser()) && !sessionUser.getUpVotedComments().contains(comment) && !sessionUser.getDownVotedComments().contains(comment)) {
				comment.incrementVotesUp();
				CommentReplyDao.updateComment(comment);
				sessionUser.getUpVotedComments().add(comment);
				SpockUserDao.updateUser(sessionUser);
			}
			ObjectMapper mapper = new ObjectMapper();
			return mapper.writeValueAsString(comment.getVotesUp());
		} catch(NumberFormatException nfe) {
			nfe.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return result;
	}

	@ResponseBody
	@RequestMapping(value="/voteDownComment", method = RequestMethod.POST, produces = "application/json")
	public String voteDownComment(HttpServletRequest request, @RequestParam("courseId") String courseIdStr, @RequestParam("commentId") String commentIdStr) {
		String result = "";
		//check authentication
		if(!SpockUtil.isUserAuthenticated(request)) {
			return result;
		}
		SpockUser sessionUser = SpockUtil.getUserFromSession(request);
		try {
			Integer courseId = Integer.parseInt(courseIdStr);
			Integer commentId = Integer.parseInt(commentIdStr);
			Comment comment = CommentReplyDao.getComment(commentId);
			if(sessionUser.hasCourseAccess(courseId) && !sessionUser.equals(comment.getUser()) && !sessionUser.getDownVotedComments().contains(comment) && !sessionUser.getUpVotedComments().contains(comment)) {
				comment.incrementVotesDown();
				CommentReplyDao.updateComment(comment);
				sessionUser.getDownVotedComments().add(comment);
				SpockUserDao.updateUser(sessionUser);
			}
			ObjectMapper mapper = new ObjectMapper();
			return mapper.writeValueAsString(comment.getVotesDown());
		} catch(NumberFormatException nfe) {
			nfe.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return result;
	}

	@ResponseBody
	@RequestMapping(value="/voteUpReply", method = RequestMethod.POST, produces = "application/json")
	public String voteUpReply(HttpServletRequest request, @RequestParam("courseId") String courseIdStr, @RequestParam("replyId") String replyIdStr) {
		String result = "";
		//check authentication
		if(!SpockUtil.isUserAuthenticated(request)) {
			return result;
		}
		SpockUser sessionUser = SpockUtil.getUserFromSession(request);
		try {
			Integer courseId = Integer.parseInt(courseIdStr);
			Integer replyId = Integer.parseInt(replyIdStr);
			Reply reply = CommentReplyDao.getReply(replyId);
			if(sessionUser.hasCourseAccess(courseId) && !sessionUser.equals(reply.getUser()) && !sessionUser.getUpVotedReplies().contains(reply) && !sessionUser.getDownVotedReplies().contains(reply)) {
				reply.incrementVotesUp();
				CommentReplyDao.updateReply(reply);
				sessionUser.getUpVotedReplies().add(reply);
				SpockUserDao.updateUser(sessionUser);
			}
			ObjectMapper mapper = new ObjectMapper();
			return mapper.writeValueAsString(reply.getVotesUp());
		} catch(NumberFormatException nfe) {
			nfe.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return result;
	}

	@ResponseBody
	@RequestMapping(value="/voteDownReply", method = RequestMethod.POST, produces = "application/json")
	public String voteDownReply(HttpServletRequest request, @RequestParam("courseId") String courseIdStr, @RequestParam("replyId") String replyIdStr) {
		String result = "";
		//check authentication
		if(!SpockUtil.isUserAuthenticated(request)) {
			return result;
		}
		SpockUser sessionUser = SpockUtil.getUserFromSession(request);
		try {
			Integer courseId = Integer.parseInt(courseIdStr);
			Integer replyId = Integer.parseInt(replyIdStr);
			Reply reply = CommentReplyDao.getReply(replyId);
			if(sessionUser.hasCourseAccess(courseId) && !sessionUser.equals(reply.getUser()) && !sessionUser.getDownVotedReplies().contains(reply) && !sessionUser.getUpVotedReplies().contains(reply)) {
				reply.incrementVotesDown();
				CommentReplyDao.updateReply(reply);
				sessionUser.getDownVotedReplies().add(reply);
				SpockUserDao.updateUser(sessionUser);
			}
			ObjectMapper mapper = new ObjectMapper();
			return mapper.writeValueAsString(reply.getVotesDown());
		} catch(NumberFormatException nfe) {
			nfe.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return result;
	}

	@RequestMapping(value="/initializeNewIcons", method=RequestMethod.GET)
	public String initializeNewIcons(ModelMap model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
		//check authentication
		if(!SpockUtil.isUserAuthenticated(request)) {
			return SpockUtil.notAuthenticatedRedirect(request, redirectAttributes);
		}
		SpockUser sessionUser = (SpockUser)request.getSession().getAttribute("user");

		try {
			UserType userType = sessionUser.getUserType();
			if (!userType.equals(UserType.SuperUser)) {
				return SpockUtil.noAccessRedirect(sessionUser, redirectAttributes);
			}
			int icons = CommentReplyDao.addNewIcons(request.getServletContext().getRealPath("/"));
			if(icons != 0) {
				redirectAttributes.addFlashAttribute("success", icons + " new icon(s) initialized successfully");
			} else {
				redirectAttributes.addFlashAttribute("error", "No new icons initialized");
			}
			return "redirect:/user/" + sessionUser.getSpockUserId();
			} catch(NumberFormatException nfe) {
			nfe.printStackTrace();
			}
		return SpockUtil.noAccessRedirect(sessionUser, redirectAttributes);
	}

	@MessageMapping("/generate/comment/{lectureId}")
	public void generateAchievementNotification(@DestinationVariable String lectureId, Comment comment) {
		template.setUserDestinationPrefix("/spock/");
		template.convertAndSend("/spock/socket/comment/" + lectureId, comment);
	}

	@MessageMapping("/generate/reply/{lectureId}")
	public void generateAchievementNotification(@DestinationVariable String lectureId, Reply reply) {
		template.setUserDestinationPrefix("/spock/");
		template.convertAndSend("/spock/socket/reply/" + lectureId, reply);
	}

	@MessageMapping("/generate/reply/notifications")
	public void generateUserNotifications(Reply reply) {
		template.setUserDestinationPrefix("/spock/");

		Comment comment = CommentReplyDao.getComment(reply.getCommentId());
		//reset reply from database to map user
		reply = CommentReplyDao.getReply(reply.getReplyId());
		SpockUser user = comment.getUser();
        String url = "/spock/lecture/" + comment.getLecture().getLectureId() + "?timestamp=" + comment.getVideoTimestamp();
		Notification notification;
		Integer commentId = comment.getCommentId();
        //notify commenter if not same as replier
        if(!user.equals(reply.getUser())) {
        	notification = NotificationDao.retrieveNotification(user, NotificationType.COMMENT_REPLY, commentId);
        	if(notification != null ) {
        		notification.setCount(notification.getCount()+1);
				NotificationDao.updateNotification(notification);
			} else {
        		 notification = new Notification(user, "Your comment received a reply", url, NotificationType.COMMENT_REPLY, commentId);
        		 NotificationDao.addNotification(notification);
        		 notification.setCount(1);
        		 template.convertAndSend("/spock/socket/user/" + user.getSpockUserId(), notification);
        	}
			if(comment.getEmailOnCommentReply()) {
				SpockUtil.sendEmailNotification(notification);
			}
        }

		//notify others who replied
		Set<SpockUser> repliers = new HashSet<SpockUser>();
		for(Reply r : comment.getReplies()) {
			repliers.add(r.getUser());
		}
		//remove commenter and user who made reply
		repliers.remove(user);
		repliers.remove(reply.getUser());
		
		for(SpockUser replier : repliers) {
			notification = NotificationDao.retrieveNotification(user, NotificationType.REPLY_REPLY, commentId);
			if(notification!=null) {
				notification.setCount(notification.getCount()+1);
				NotificationDao.updateNotification(notification);
			} else {
				notification = new Notification(replier, "Activity in a thread you participated in", url, NotificationType.REPLY_REPLY, commentId);
				NotificationDao.addNotification(notification);
				notification.setCount(1);
				template.convertAndSend("/spock/socket/user/" + replier.getSpockUserId(), notification);
			}
			if(reply.getEmailOnReplyReply()) {
				SpockUtil.sendEmailNotification(notification);
			}
		}
	}
}

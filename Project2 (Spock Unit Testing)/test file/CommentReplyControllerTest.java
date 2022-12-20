package spock.controllers;

import org.codesmell.jar.model.CodesmellCourse;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ModelMap;
import spock.dao.*;
import spock.model.*;

import java.util.Date;
import java.util.List;


import static org.junit.jupiter.api.Assertions.*;

public class CommentReplyControllerTest {

    private static CommentReplyController commentReplyController;
    private static ModelMap model;

    //users
    private static SpockUser studentUser;
    private static SpockUser replyUser;
    private static SpockUser studentUserNoEnroll;

    private static Course course1;
    private static Lecture lecture1;
    private static Unit unit1;
    private static Comment comment1;

    @BeforeAll
    public static void setup() {
        commentReplyController = new CommentReplyController();
    }

    @AfterAll
    public static void tearDown() {
    }

    @BeforeEach
    public void beforeEach(){
        model = new ModelMap();

        //create date
        Date startDate = new Date(1666827858);
        Date earlyDate = new Date(1667432658);
        Date deadlineDate = new Date(1921197689000L);

        //creating user
        studentUser = new SpockUser("studentUser@testmail.com","salt","hash","stu","student", UserType.Student);
        replyUser = new SpockUser("replyUser@replymail.com","salt","hash","re","ply", UserType.Student);
        studentUserNoEnroll = new SpockUser("studentUser@bunkmail.com","salt","hash","no","student", UserType.Student);

        //create course
        course1 = new Course("d1", "1", "1", CodesmellCourse.Semester.Fall, "1", "name1", AchievementDao.getAllAchievements());

        //create unit
        unit1 = new Unit(1, "unit1", startDate, earlyDate, deadlineDate);

        //create lecture
        lecture1 = new Lecture("lecture1", "url1", unit1, 1, startDate, earlyDate, deadlineDate);

        //set relationships
        course1.addSpockUser(studentUser);
        course1.addSpockUser(replyUser);

        course1.addUnit(unit1);

        unit1.setCourse(course1);

        unit1.addLecture(lecture1);

        lecture1.setUnit(unit1);

        comment1 = new Comment();
        comment1.setLecture(lecture1);
        comment1.setText("parent comment");
        comment1.setUser(studentUser);
        comment1.setVideoTimestamp("88888888");
        comment1.setVotesUp(0);
        comment1.setVotesDown(0);

        //adding everything to the database
        SpockUserDao.addUser(studentUser);
        SpockUserDao.addUser(studentUserNoEnroll);
        SpockUserDao.addUser(replyUser);

        CourseUnitLectureDao.addCourse(course1);

        CourseUnitLectureDao.addUnit(unit1, course1);

        CourseUnitLectureDao.addLecture(lecture1, unit1);

        CommentReplyDao.addComment(comment1);

        //updating everything to the recently added entities in the database
        studentUser = SpockUserDao.getUser(studentUser.getSpockUserId());
        studentUserNoEnroll = SpockUserDao.getUser(studentUserNoEnroll.getSpockUserId());
        replyUser = SpockUserDao.getUser(replyUser.getSpockUserId());

        course1 = CourseUnitLectureDao.getCourse(course1.getCourseId());

        unit1 = CourseUnitLectureDao.getUnit(unit1.getUnitId());

        lecture1 = CourseUnitLectureDao.getLecture(lecture1.getLectureId());

        comment1 = CommentReplyDao.getComment(comment1.getCommentId());
    }

    @Test
    public void addCommentTest(){
        //setup
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        MockHttpServletRequest request3 = new MockHttpServletRequest();

        //requests
        //posting proper comment
        request1.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request1.setRequestURI(":8000/spock/comment");
        request1.getSession().setAttribute("user", studentUser);

        //user not auth'd
        request2.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request2.setRequestURI(":8000/spock/comment");
        request2.getSession().setAttribute("user", null);

        //user not enrolled in course
        request3.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request3.setRequestURI(":8000/spock/comment");
        request3.getSession().setAttribute("user", studentUserNoEnroll);

        //responses
        //standard
        String response1a = commentReplyController.addComment(request1, Integer.toString(lecture1.getLectureId()), "comment text 1a", "99999999", false, false);
        //emailOnReply
        String response1b = commentReplyController.addComment(request1, Integer.toString(lecture1.getLectureId()), "comment text 1b", "99999999", true, false);
        //isAnonymous
        String response1c = commentReplyController.addComment(request1, Integer.toString(lecture1.getLectureId()), "comment text 1c", "99999999", false, true);
        //both
        String response1d = commentReplyController.addComment(request1, Integer.toString(lecture1.getLectureId()), "comment text 1d", "99999999", true, true);

        //NumberFormatException
        String responseNFE = commentReplyController.addComment(request1, "jl;kfsda", "comment text NFE", "99999999", false, true);

        //user does not exist
        String response2 = commentReplyController.addComment(request2, Integer.toString(lecture1.getLectureId()), "comment text 2", "99999999", false, true);

        //user not enrolled in course
        String response3 = commentReplyController.addComment(request3, Integer.toString(lecture1.getLectureId()), "comment text 3", "99999999", false, true);

        request1.close();
        request2.close();
        request3.close();

        Lecture refLecture = CourseUnitLectureDao.getLecture(lecture1.getLectureId());

        //assert
        assertEquals(refLecture.getComments().size(), 5);

        assertTrue(response1a.contains("\"text\":\"comment text 1a\""));
        assertTrue(response1a.contains("\"videoTimestamp\":\"99999999\""));
        assertTrue(response1a.contains("\"userName\":\"stu student\""));
        assertTrue(response1a.contains("\"emailOnCommentReply\":false,\"anonymous\":false"));

        assertTrue(response1b.contains("\"text\":\"comment text 1b\""));
        assertTrue(response1b.contains("\"videoTimestamp\":\"99999999\""));
        assertTrue(response1b.contains("\"userName\":\"stu student\""));
        assertTrue(response1b.contains("\"emailOnCommentReply\":true,\"anonymous\":false"));

        assertTrue(response1c.contains("\"text\":\"comment text 1c\""));
        assertTrue(response1c.contains("\"videoTimestamp\":\"99999999\""));
        assertTrue(response1c.contains("\"userName\":\"\""));
        assertTrue(response1c.contains("\"emailOnCommentReply\":false,\"anonymous\":true"));

        assertTrue(response1d.contains("\"text\":\"comment text 1d\""));
        assertTrue(response1d.contains("\"videoTimestamp\":\"99999999\""));
        assertTrue(response1d.contains("\"userName\":\"\""));
        assertTrue(response1d.contains("\"emailOnCommentReply\":true,\"anonymous\":true"));

        assertEquals("", response2);
        assertEquals("", responseNFE);
        assertEquals("", response3);
    }
    @Test
    public void addReplyTest() {
        //setup
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        MockHttpServletRequest request3 = new MockHttpServletRequest();

        //requests
        //posting proper reply
        request1.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request1.setRequestURI(":8000/spock/reply");
        request1.getSession().setAttribute("user", replyUser);

        //user not auth'd
        request2.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request2.setRequestURI(":8000/spock/reply");
        request2.getSession().setAttribute("user", null);

        //user not enrolled in course
        request3.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request3.setRequestURI(":8000/spock/reply");
        request3.getSession().setAttribute("user", studentUserNoEnroll);

        //responses
        //standard
        String response1a = commentReplyController.addReply(request1, Integer.toString(lecture1.getLectureId()),  Integer.toString(comment1.getCommentId()), "reply text 1a", false, false);
        //emailOnReply
        String response1b = commentReplyController.addReply(request1, Integer.toString(lecture1.getLectureId()),  Integer.toString(comment1.getCommentId()), "reply text 1b", true, false);
        //isAnonymous
        String response1c = commentReplyController.addReply(request1, Integer.toString(lecture1.getLectureId()),  Integer.toString(comment1.getCommentId()), "reply text 1c", false, true);
        //both
        String response1d = commentReplyController.addReply(request1, Integer.toString(lecture1.getLectureId()),  Integer.toString(comment1.getCommentId()), "reply text 1d", true, true);

        //non-int input on lectureId (will pass since method doesn't reference it directly) and commentId (will fail)
        String responseOddLec = commentReplyController.addReply(request1, "fdehankpj",  Integer.toString(comment1.getCommentId()), "lectureId doesn't matter", false, false);
        String responseNFEcom = commentReplyController.addReply(request1, Integer.toString(lecture1.getLectureId()),  "fdehankpj", "error on commentId", false, false);

        //user does not exist
        String response2 = commentReplyController.addReply(request2, Integer.toString(lecture1.getLectureId()),  Integer.toString(comment1.getCommentId()), "user null", false, false);

        //user not enrolled in course
        String response3 = commentReplyController.addReply(request3, Integer.toString(lecture1.getLectureId()),  Integer.toString(comment1.getCommentId()), "user lacks access", false, false);

        request1.close();
        request2.close();
        request3.close();

        Lecture refLecture = CourseUnitLectureDao.getLecture(lecture1.getLectureId());
        Comment refComment = CommentReplyDao.getComment(comment1.getCommentId());

        //assert
        assertEquals(refLecture.getComments().size(), 1);
        assertEquals(refComment.getReplies().size(), 5);

        assertTrue(response1a.contains("\"text\":\"reply text 1a\""));
        assertTrue(response1a.contains("\"userName\":\"re ply\""));
        assertTrue(response1a.contains("\"emailOnReplyReply\":false,\"anonymous\":false"));

        assertTrue(response1b.contains("\"text\":\"reply text 1b\""));
        assertTrue(response1b.contains("\"userName\":\"re ply\""));
        assertTrue(response1b.contains("\"emailOnReplyReply\":true,\"anonymous\":false"));

        assertTrue(response1c.contains("\"text\":\"reply text 1c\""));
        assertTrue(response1c.contains("\"userName\":\"\""));
        assertTrue(response1c.contains("\"emailOnReplyReply\":false,\"anonymous\":true"));

        assertTrue(response1d.contains("\"text\":\"reply text 1d\""));
        assertTrue(response1d.contains("\"userName\":\"\""));
        assertTrue(response1d.contains("\"emailOnReplyReply\":true,\"anonymous\":true"));

        assertTrue(responseOddLec.contains("\"text\":\"lectureId doesn&#39;t matter\""));
        assertTrue(responseOddLec.contains("\"userName\":\"re ply\""));
        assertTrue(responseOddLec.contains("\"emailOnReplyReply\":false,\"anonymous\":false"));
        
        assertEquals("", response2);
        assertEquals("", responseNFEcom);
        assertEquals("", response3);
    }

    @Test
    public void voteUpCommentTest() {
        //setup
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        MockHttpServletRequest request3 = new MockHttpServletRequest();
        MockHttpServletRequest request4 = new MockHttpServletRequest();

        request1.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request1.setRequestURI(":8000/spock/voteUpComment");
        request1.getSession().setAttribute("user", replyUser);

        request2.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request2.setRequestURI(":8000/spock/voteUpComment");
        request2.getSession().setAttribute("user", studentUserNoEnroll);

        request3.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request3.setRequestURI(":8000/spock/voteUpComment");
        request3.getSession().setAttribute("user", studentUser);

        request4.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request4.setRequestURI(":8000/spock/voteUpComment");
        request4.getSession().setAttribute("user", null);

        //initial comment list before any requests
        int replyUserCommentListPre = replyUser.getUpVotedComments().size();

        //proper request with course access
        String response1a = commentReplyController.voteUpComment(request1, Integer.toString(course1.getCourseId()),  Integer.toString(comment1.getCommentId()));
        //things should be different now
        int replyUserCommentListPost1 = replyUser.getUpVotedComments().size();

        //ASSERT now before more is added
        assertEquals(CommentReplyDao.getComment(comment1.getCommentId()).getVoteRating(), 1);
        assertNotSame(replyUserCommentListPre, replyUserCommentListPost1);

        //incorrect courseId
        String response1b = commentReplyController.voteUpComment(request1, "ajkl;dsj",  Integer.toString(comment1.getCommentId()));
        //incorrect commentId
        String response1c = commentReplyController.voteUpComment(request1, Integer.toString(course1.getCourseId()),  "ajkl;dsj");

        //things should NOT be different now
        int replyUserCommentListPost2 = replyUser.getUpVotedComments().size();

        //ASSERT now to make sure nothing has changed from intentional errors
        assertEquals(CommentReplyDao.getComment(comment1.getCommentId()).getVoteRating(), 1);
        assertSame(replyUserCommentListPost1, replyUserCommentListPost2);

        //user already upvoted
        String response1d = commentReplyController.voteUpComment(request1, Integer.toString(course1.getCourseId()),  Integer.toString(comment1.getCommentId()));
        //things should NOT be different now
        replyUserCommentListPost2 = replyUser.getUpVotedComments().size();

        //ASSERT after to make sure nothing has changed
        assertEquals(CommentReplyDao.getComment(comment1.getCommentId()).getVoteRating(), 1);
        assertSame(replyUserCommentListPost1, replyUserCommentListPost2);

        //initial comment list before possible changes
        int studentUserNoEnrollCommentListPre = replyUser.getUpVotedComments().size();
        //request without course access
        String response2 = commentReplyController.voteUpComment(request2, Integer.toString(course1.getCourseId()),  Integer.toString(comment1.getCommentId()));
        //things should NOT be different now
        int studentUserNoEnrollCommentListPost = replyUser.getUpVotedComments().size();

        //ASSERT after to make sure nothing has changed
        assertSame(studentUserNoEnrollCommentListPre, studentUserNoEnrollCommentListPost);

        //check again before anything changes
        assertEquals(CommentReplyDao.getComment(comment1.getCommentId()).getVoteRating(), 1);
        //request from comment writer
        String response3 = commentReplyController.voteUpComment(request3, Integer.toString(course1.getCourseId()),  Integer.toString(comment1.getCommentId()));
        //shouldn't have changed since they wrote the comment in the first place
        assertSame(CommentReplyDao.getComment(comment1.getCommentId()).getVoteRating(), 1);

        //request with null user
        String response4 = commentReplyController.voteUpComment(request4, Integer.toString(course1.getCourseId()),  Integer.toString(comment1.getCommentId()));

        //close requests as they're no longer needed
        request1.close();
        request2.close();
        request3.close();
        request4.close();

        //ASSERT the rest
        assertEquals("", response1b);
        assertEquals("", response1c);
        assertEquals("", response4);
    }

    @Test
    public void voteDownCommentTest() {
        //setup
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        MockHttpServletRequest request3 = new MockHttpServletRequest();
        MockHttpServletRequest request4 = new MockHttpServletRequest();

        request1.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request1.setRequestURI(":8000/spock/voteDownComment");
        request1.getSession().setAttribute("user", replyUser);

        request2.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request2.setRequestURI(":8000/spock/voteDownComment");
        request2.getSession().setAttribute("user", studentUserNoEnroll);

        request3.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request3.setRequestURI(":8000/spock/voteDownComment");
        request3.getSession().setAttribute("user", studentUser);

        request4.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request4.setRequestURI(":8000/spock/voteDownComment");
        request4.getSession().setAttribute("user", null);

        //initial comment list before any requests
        int replyUserCommentListPre = replyUser.getDownVotedComments().size();

        //proper request with course access
        String response1a = commentReplyController.voteDownComment(request1, Integer.toString(course1.getCourseId()),  Integer.toString(comment1.getCommentId()));
        //things should be different now
        int replyUserCommentListPost1 = replyUser.getDownVotedComments().size();

        //ASSERT now before more is added
        assertEquals(CommentReplyDao.getComment(comment1.getCommentId()).getVoteRating(), -1);
        assertNotSame(replyUserCommentListPre, replyUserCommentListPost1);

        //incorrect courseId
        String response1b = commentReplyController.voteDownComment(request1, "ajkl;dsj",  Integer.toString(comment1.getCommentId()));
        //incorrect commentId
        String response1c = commentReplyController.voteDownComment(request1, Integer.toString(course1.getCourseId()),  "ajkl;dsj");

        //things should NOT be different now
        int replyUserCommentListPost2 = replyUser.getDownVotedComments().size();

        //ASSERT now to make sure nothing has changed from intentional errors
        assertEquals(CommentReplyDao.getComment(comment1.getCommentId()).getVoteRating(), -1);
        assertSame(replyUserCommentListPost1, replyUserCommentListPost2);

        //user already downvoted
        String response1d = commentReplyController.voteDownComment(request1, Integer.toString(course1.getCourseId()),  Integer.toString(comment1.getCommentId()));
        //things should NOT be different now
        replyUserCommentListPost2 = replyUser.getDownVotedComments().size();

        //ASSERT after to make sure nothing has changed
        assertEquals(CommentReplyDao.getComment(comment1.getCommentId()).getVoteRating(), -1);
        assertSame(replyUserCommentListPost1, replyUserCommentListPost2);

        //initial comment list before possible changes
        int studentUserNoEnrollCommentListPre = replyUser.getDownVotedComments().size();
        //request without course access
        String response2 = commentReplyController.voteUpComment(request2, Integer.toString(course1.getCourseId()),  Integer.toString(comment1.getCommentId()));
        //things should NOT be different now
        int studentUserNoEnrollCommentListPost = replyUser.getDownVotedComments().size();

        //ASSERT after to make sure nothing has changed
        assertSame(studentUserNoEnrollCommentListPre, studentUserNoEnrollCommentListPost);

        //check again before anything changes
        assertEquals(CommentReplyDao.getComment(comment1.getCommentId()).getVoteRating(), -1);
        //request from comment writer
        String response3 = commentReplyController.voteDownComment(request3, Integer.toString(course1.getCourseId()),  Integer.toString(comment1.getCommentId()));
        //shouldn't have changed since they wrote the comment in the first place
        assertSame(CommentReplyDao.getComment(comment1.getCommentId()).getVoteRating(), -1);

        //request with null user
        String response4 = commentReplyController.voteDownComment(request4, Integer.toString(course1.getCourseId()),  Integer.toString(comment1.getCommentId()));

        //close requests as they're no longer needed
        request1.close();
        request2.close();
        request3.close();
        request4.close();

        //ASSERT the rest
        assertEquals("", response1b);
        assertEquals("", response1c);
        assertEquals("", response4);
    }

    @Test
    public void voteUpReplyTest() {
        //declare requests
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        MockHttpServletRequest request3 = new MockHttpServletRequest();
        MockHttpServletRequest request4 = new MockHttpServletRequest();

        //create reply for testing
        request1.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request1.setRequestURI(":8000/spock/reply");
        request1.getSession().setAttribute("user", replyUser);

        String replyResponse = commentReplyController.addReply(request1, Integer.toString(lecture1.getLectureId()),  Integer.toString(comment1.getCommentId()), "reply text", false, false);

        //redeclare request 1 and form test cases
        request1 = new MockHttpServletRequest();

        request1.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request1.setRequestURI(":8000/spock/voteUpReply");
        request1.getSession().setAttribute("user", studentUser);

        request2.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request2.setRequestURI(":8000/spock/voteUpReply");
        request2.getSession().setAttribute("user", replyUser);

        request3.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request3.setRequestURI(":8000/spock/voteUpReply");
        request3.getSession().setAttribute("user", studentUserNoEnroll);

        request4.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request4.setRequestURI(":8000/spock/voteUpReply");
        request4.getSession().setAttribute("user", null);

        //initial comment list before any requests
        int studentUserReplyListPre = studentUser.getUpVotedReplies().size();

        //bad comment ID
        String response1a = commentReplyController.voteUpReply(request1, "fdkasjhp",  Integer.toString(CommentReplyDao.getComment(comment1.getCommentId()).getReplies().get(0).getReplyId()));
        //bad reply ID
        String response1b = commentReplyController.voteUpReply(request1, Integer.toString(course1.getCourseId()),  "fdkasjhp");

        //proper request with course access
        String response1c = commentReplyController.voteUpReply(request1, Integer.toString(course1.getCourseId()),  Integer.toString(CommentReplyDao.getComment(comment1.getCommentId()).getReplies().get(0).getReplyId()));

        //ASSERT now before more is added
        assertEquals(CommentReplyDao.getComment(comment1.getCommentId()).getReplies().get(0).getVotesUp(),1);
        assertEquals(studentUserReplyListPre, 0);
        assertEquals(studentUser.getUpVotedReplies().size(), 1);

        //reply writer shouldn't be able to upvote their own reply
        String response2 = commentReplyController.voteUpReply(request2, Integer.toString(course1.getCourseId()),  Integer.toString(CommentReplyDao.getComment(comment1.getCommentId()).getReplies().get(0).getReplyId()));

        //user lacks course access
        String response3 = commentReplyController.voteUpReply(request3, Integer.toString(course1.getCourseId()),  Integer.toString(CommentReplyDao.getComment(comment1.getCommentId()).getReplies().get(0).getReplyId()));

        //null user
        String response4 = commentReplyController.voteUpReply(request4, Integer.toString(course1.getCourseId()),  Integer.toString(CommentReplyDao.getComment(comment1.getCommentId()).getReplies().get(0).getReplyId()));

        assertEquals(replyUser.getUpVotedReplies().size(), 0);
        assertEquals(studentUserNoEnroll.getUpVotedReplies().size(), 0);

        assertEquals(response1a, "");
        assertEquals(response1b, "");
        assertEquals(response1c, "1");
        assertEquals(response2, "1");
        assertEquals(response3, "1");
        assertEquals(response4, "");
    }

    @Test
    public void voteDownReplyTest() {
        //declare requests
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        MockHttpServletRequest request3 = new MockHttpServletRequest();
        MockHttpServletRequest request4 = new MockHttpServletRequest();

        //create reply for testing
        request1.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request1.setRequestURI(":8000/spock/reply");
        request1.getSession().setAttribute("user", replyUser);

        String replyResponse = commentReplyController.addReply(request1, Integer.toString(lecture1.getLectureId()),  Integer.toString(comment1.getCommentId()), "reply text", false, false);

        //redeclare request 1 and form test cases
        request1 = new MockHttpServletRequest();

        request1.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request1.setRequestURI(":8000/spock/voteDownReply");
        request1.getSession().setAttribute("user", studentUser);

        request2.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request2.setRequestURI(":8000/spock/voteDownReply");
        request2.getSession().setAttribute("user", replyUser);

        request3.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request3.setRequestURI(":8000/spock/voteDownReply");
        request3.getSession().setAttribute("user", studentUserNoEnroll);

        request4.addHeader("referer", "http://localhost:8000/spock/lecture/" + lecture1.getLectureId());
        request4.setRequestURI(":8000/spock/voteDownReply");
        request4.getSession().setAttribute("user", null);

        //initial comment list before any requests
        int studentUserReplyListPre = studentUser.getDownVotedReplies().size();

        //bad comment ID
        String response1a = commentReplyController.voteDownReply(request1, "fdkasjhp",  Integer.toString(CommentReplyDao.getComment(comment1.getCommentId()).getReplies().get(0).getReplyId()));
        //bad reply ID
        String response1b = commentReplyController.voteDownReply(request1, Integer.toString(course1.getCourseId()),  "fdkasjhp");

        //proper request with course access
        String response1c = commentReplyController.voteDownReply(request1, Integer.toString(course1.getCourseId()),  Integer.toString(CommentReplyDao.getComment(comment1.getCommentId()).getReplies().get(0).getReplyId()));

        //ASSERT now before more is added
        assertEquals(CommentReplyDao.getComment(comment1.getCommentId()).getReplies().get(0).getVotesDown(),1);
        assertEquals(studentUserReplyListPre, 0);
        assertEquals(studentUser.getDownVotedReplies().size(), 1);

        //reply writer shouldn't be able to upvote their own reply
        String response2 = commentReplyController.voteDownReply(request2, Integer.toString(course1.getCourseId()),  Integer.toString(CommentReplyDao.getComment(comment1.getCommentId()).getReplies().get(0).getReplyId()));

        //user lacks course access
        String response3 = commentReplyController.voteDownReply(request3, Integer.toString(course1.getCourseId()),  Integer.toString(CommentReplyDao.getComment(comment1.getCommentId()).getReplies().get(0).getReplyId()));

        //null user
        String response4 = commentReplyController.voteDownReply(request4, Integer.toString(course1.getCourseId()),  Integer.toString(CommentReplyDao.getComment(comment1.getCommentId()).getReplies().get(0).getReplyId()));

        assertEquals(replyUser.getDownVotedReplies().size(), 0);
        assertEquals(studentUserNoEnroll.getDownVotedReplies().size(), 0);

        assertEquals(response1a, "");
        assertEquals(response1b, "");
        assertEquals(response1c, "1");
        assertEquals(response2, "1");
        assertEquals(response3, "1");
        assertEquals(response4, "");
    }

    @AfterEach
    public void afterEach(){
        //remove associations
        comment1.setUser(null);
        comment1.setLecture(null);
        comment1.setReplies(null);
        CommentReplyDao.updateComment(comment1);

        SpockUserDao.updateUser(studentUser);

        //delete lecture
        CourseUnitLectureDao.deleteLecture(lecture1);

        //delete unit
        CourseUnitLectureDao.deleteUnit(unit1);

        //delete course
        CourseUnitLectureDao.deleteCourse(course1);

        //refresh all users
        studentUser = SpockUserDao.getUser(studentUser.getSpockUserId());

        //delete all users
        SpockUserDao.deleteUser(studentUser);
    }
}

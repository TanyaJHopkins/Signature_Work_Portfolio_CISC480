package spock.dao;

import spock.model.*;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CommentReplyDao extends HibernateDao {
	
	public static Comment getComment(Integer commentId) {
		Session session = sessionFactory.openSession();
		Transaction t = null;	
		Comment comment = null;
		try {
			t = session.beginTransaction();
			if(commentId != null) {
			  comment = (Comment) session.get(Comment.class, commentId);
			}
		} catch (HibernateException e) {
			if (t != null) {
				t.rollback();
			}
			e.printStackTrace();
		} finally {
			session.close();
		}
		return comment;
	}

	public static int getNumUserLectureComments(SpockUser user, Lecture lecture) {
		int numLectureComments = 0;
		Session session = sessionFactory.openSession();
		Transaction t = null;
		try {
			t = session.beginTransaction();
			Query<Comment> query = session.createQuery("FROM Comment WHERE lecture = :lecture AND user = :user", Comment.class);
			query.setParameter("lecture", lecture);
			query.setParameter("user", user);
			numLectureComments = query.list().size();
		} catch (HibernateException e) {
			if (t != null) {
				t.rollback();
			}
			e.printStackTrace();
		} finally {
			session.close();
		}
		return numLectureComments;
	}
	
	public static Reply getReply(Integer replyId) {
		Session session = sessionFactory.openSession();
		Transaction t = null;	
		Reply reply = null;
		try {
			t = session.beginTransaction();
			if(replyId != null) {
			  reply = (Reply) session.get(Reply.class, replyId);
			}
		} catch (HibernateException e) {
			if (t != null) {
				t.rollback();
			}
			e.printStackTrace();
		} finally {
			session.close();
		}
		return reply;
	}
	
	public static boolean addComment(Comment comment) {
		boolean success = false;
		Session session = sessionFactory.openSession();
		Transaction t = null;
		try {
			t = session.beginTransaction();
			session.persist(comment);
			Lecture lecture = comment.getLecture();
			lecture.addComment(comment);
			session.refresh(lecture);
			t.commit();
			session.refresh(lecture.getUnit().getCourse());
			success = true;
		} catch (HibernateException e) {
			if (t != null) {
				t.rollback();
			}
			e.printStackTrace();
		} finally {
			session.close();
		}
		return success;
	}

	public static boolean addReply(Reply reply) {
		boolean success = false;
		Session session = sessionFactory.openSession();
		Transaction t = null;
		try {
			t = session.beginTransaction();
			session.persist(reply);
			t.commit();
			success = true;
		} catch (HibernateException e) {
			if (t != null) {
				t.rollback();
			}
			e.printStackTrace();
		} finally {
			session.close();
		}
		return success;
	}
	
	public static Icon getStudentIcon(SpockUser user, Lecture lecture, String servletContextPath) {
		Icon icon = getStudentLectureIcon(user, lecture);
		if(icon == null) {
			Session session = sessionFactory.openSession();
			Transaction t = null;
			try {
				t = session.beginTransaction();	
				List<Icon> icons = getAllIcons(session, servletContextPath);
				List<Icon> usedLectureIcons = getUsedLectureIcons(lecture, session);
				icons.removeAll(usedLectureIcons);
				Random r = new Random();
				icon = icons.get(r.nextInt(icons.size()));
			} catch (Exception e) {
				if (t != null) {
					t.rollback();
				}
				e.printStackTrace();
			} finally {
				session.close();
			}
		}
		return icon;
	}
	
	public static boolean updateComment(Comment comment) {
		boolean success = false;
		Session session = sessionFactory.openSession();
		Transaction t = null;
		try {
			t = session.beginTransaction();
			session.merge(comment);
			t.commit();
			session.refresh(comment);
			success = true;
		} catch (HibernateException e) {
			if (t != null) {
				t.rollback();
			}
			e.printStackTrace();
		} finally {
			session.close();
		}
		return success;
	}

	public static boolean deleteComment(Comment comment) {
		boolean success = false;
		Session session = sessionFactory.openSession();
		Transaction t = null;
		try {
			t = session.beginTransaction();
			session.remove(comment);
			t.commit();
			success = true;
		} catch (HibernateException e) {
			if (t != null) {
				t.rollback();
			}
			e.printStackTrace();
		} finally {
			session.close();
		}
		return success;
	}

	public static boolean updateReply(Reply reply) {
		boolean success = false;
		Session session = sessionFactory.openSession();
		Transaction t = null;
		try {
			t = session.beginTransaction();
			session.merge(reply);
			t.commit();
			success = true;
		} catch (HibernateException e) {
			if (t != null) {
				t.rollback();
			}
			e.printStackTrace();
		} finally {
			session.close();
		}
		return success;
	}
	
	@SuppressWarnings("unchecked")
	private static Icon getStudentLectureIcon(SpockUser user, Lecture lecture) {
		Session session = sessionFactory.openSession();
		Transaction t = null;	
		Icon icon = null;
		try {
			t = session.beginTransaction();
			Query<Comment> query = session.createQuery("FROM Comment WHERE lecture = :lecture AND user = :user", Comment.class);
			query.setParameter("lecture", lecture);
			query.setParameter("user", user);
			if(!query.list().isEmpty()) {
				icon = query.list().get(0).getIcon();
			} else {
				Query<Reply> replyQuery = session.createQuery("FROM Reply r WHERE r.user = :user AND r.commentId=(SELECT commentId FROM Comment WHERE r IN elements(replies) AND lecture = :lecture)", Reply.class);
				replyQuery.setParameter("lecture", lecture);
				replyQuery.setParameter("user", user);
				if(!replyQuery.list().isEmpty()) {
					List<Reply> replies = replyQuery.list();
					icon = replies.get(0).getIcon();
				}
			}
		} catch (HibernateException e) {
			if (t != null) {
				t.rollback();
			}
			e.printStackTrace();
		} finally {
			session.close();
		}
		return icon;
	}
	
	@SuppressWarnings("unchecked")
	private static List<Icon> getUsedLectureIcons(Lecture lecture, Session session) {
		List<Icon> usedLectureIcons = new ArrayList<Icon>();
		for(Comment comment : lecture.getComments()) {
		    usedLectureIcons.add(comment.getIcon());
		    for(Reply reply : comment.getReplies()) {
		        usedLectureIcons.add(reply.getIcon());
            }
        }
		return usedLectureIcons;
	}
	
	@SuppressWarnings("unchecked")
	private static List<Icon> getAllIcons(Session session, String servletContextPath) {
		List<Icon> allIcons = new ArrayList<Icon>();
		Query<Icon> query = session.createQuery("FROM Icon", Icon.class);
		allIcons = query.list();
        if(allIcons.isEmpty()) {
            initializeAllIcons(allIcons, session, servletContextPath);
        }
		return allIcons;
	}

	private static void initializeAllIcons(List<Icon> allIcons, Session session, String servletContextPath) {
        File iconDir = new File(servletContextPath + Icon.ICON_PATH);
        if(iconDir.isDirectory()) {
            for(File file : iconDir.listFiles()) {
                Icon icon = new Icon();
                icon.setFilename(file.getName());
                allIcons.add(icon);
                addIcon(icon);
            }
        }
    }

	public static int addNewIcons(String servletContextPath) {
        int success = 0;
		Session session = sessionFactory.openSession();
		List<Icon> currentIcons = getAllIcons(session, servletContextPath);
		List<String> currentIconFilenames = new ArrayList<>();
		for(Icon icon : currentIcons) {
            currentIconFilenames.add(icon.getFilename());
        }
        File iconDir = new File(servletContextPath + Icon.ICON_PATH);
        if(iconDir.isDirectory()) {
            for(File file : iconDir.listFiles()) {
                if(!currentIconFilenames.contains(file.getName())) {
                    Icon icon = new Icon();
                    icon.setFilename(file.getName());
                    boolean worked = addIcon(icon);
                    if(worked) {
                        success ++;
                    }
                }
            }
        }
        return success;
	}

    private static boolean addIcon(Icon icon) {
        boolean success = false;
        Session session = sessionFactory.openSession();
        Transaction t = null;
        try {
            t = session.beginTransaction();
            session.persist(icon);
            t.commit();
            success = true;
        } catch (HibernateException e) {
            if (t != null) {
                t.rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }
        return success;
    }
}

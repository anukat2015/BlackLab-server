package nl.inl.blacklab.server.search;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import nl.inl.blacklab.server.BlackLabServer;

/**
 * Authentication system used for debugging.
 * 
 * Debug IPs may fake logged-in user by passing userid parameter.
 */
public class DebugAuthSystem {
	
	public User determineCurrentUser(HttpServlet servlet,
			HttpServletRequest request) {
		
		// Is client on debug IP and is there a userid parameter?
		String userId = null;
		SearchManager searchMan = ((BlackLabServer)servlet).getSearchManager();
		if (searchMan.mayOverrideUserId(request.getRemoteAddr()) && request.getParameter("userid") != null) {
			userId = request.getParameter("userid");
		}
		
		// Return the appropriate User object
		String sessionId = request.getSession().getId();
		if (userId == null || userId.length() == 0) {
			return User.anonymous(sessionId);
		}
		return User.loggedIn(userId, sessionId);
	}

}

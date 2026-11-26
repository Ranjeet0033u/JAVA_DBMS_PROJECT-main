package util;

import dao.UserDAO;
import java.sql.SQLException;
import java.util.Locale;
import model.User;

public class AuthService {

    private final UserDAO userDAO;

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User authenticate(String email, String password) throws SQLException {
        if (email == null || email.isBlank() || password == null || password.isEmpty()) {
            return null;
        }

        User user = userDAO.getUserByEmail(email.trim().toLowerCase(Locale.ROOT));
        return user != null && PasswordUtil.verifyPassword(password, user.getPasswordHash())
                ? user
                : null;
    }

    public User register(String fullName, String email, String password)
            throws SQLException, RegistrationException {
        String trimmedName = fullName == null ? "" : fullName.trim().replaceAll("\\s+", " ");
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String normalizedName = normalizeAccountName(trimmedName);

        if (trimmedName.isEmpty()) {
            throw new RegistrationException(RegistrationException.Reason.INVALID_NAME);
        }
        if (normalizedEmail.isEmpty() || !normalizedEmail.contains("@") || !normalizedEmail.contains(".")) {
            throw new RegistrationException(RegistrationException.Reason.INVALID_EMAIL);
        }
        if (password == null || password.length() < 6) {
            throw new RegistrationException(RegistrationException.Reason.WEAK_PASSWORD);
        }
        if (userDAO.normalizedNameExists(normalizedName)) {
            throw new RegistrationException(RegistrationException.Reason.DUPLICATE_NAME);
        }
        if (userDAO.emailExists(normalizedEmail)) {
            throw new RegistrationException(RegistrationException.Reason.DUPLICATE_EMAIL);
        }

        User user = new User(trimmedName, normalizedName, normalizedEmail,
                PasswordUtil.hashPassword(password));
        userDAO.registerUser(user);
        return user;
    }

    public static String normalizeAccountName(String rawName) {
        if (rawName == null) {
            return "";
        }
        return rawName.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public static class RegistrationException extends Exception {
        public enum Reason {
            INVALID_NAME,
            INVALID_EMAIL,
            WEAK_PASSWORD,
            DUPLICATE_NAME,
            DUPLICATE_EMAIL
        }

        private final Reason reason;

        public RegistrationException(Reason reason) {
            super(reason.name());
            this.reason = reason;
        }

        public Reason getReason() {
            return reason;
        }
    }
}

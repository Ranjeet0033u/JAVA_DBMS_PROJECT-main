package main;

import dao.ApplicationDAO;
import dao.ApplicationDAOImpl;
import dao.CompanyDAO;
import dao.CompanyDAOImpl;
import dao.InterviewDAO;
import dao.InterviewDAOImpl;
import dao.OfferDAO;
import dao.OfferDAOImpl;
import dao.UserDAOImpl;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import model.Application;
import model.Company;
import model.InternshipApplication;
import model.Interview;
import model.JobApplication;
import model.Offer;
import model.User;
import util.InvalidStatusException;
import util.AuthService;
import util.ReportUtil;
import util.WebServer;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static final AuthService authService = new AuthService(new UserDAOImpl());
    private static final CompanyDAO companyDAO = new CompanyDAOImpl();
    private static final ApplicationDAO applicationDAO = new ApplicationDAOImpl();
    private static final InterviewDAO interviewDAO = new InterviewDAOImpl();
    private static final OfferDAO offerDAO = new OfferDAOImpl();

    public static void main(String[] args) {
        if (args.length > 0 && "web".equalsIgnoreCase(args[0])) {
            WebServer.main(args);
            return;
        }

        User currentUser = authenticate();
        if (currentUser == null) {
            return;
        }

        boolean running = true;
        while (running) {
            printMainMenu(currentUser);
            String choice = prompt("Select an option");

            try {
                switch (choice) {
                    case "1" -> handleCompanies();
                    case "2" -> handleApplications(currentUser);
                    case "3" -> handleInterviews(currentUser);
                    case "4" -> handleOffers(currentUser);
                    case "5" -> handleFollowUps(currentUser);
                    case "6" -> handleReports(currentUser);
                    case "0" -> {
                        running = false;
                        System.out.println("\nThank you for using CareerTrack.");
                    }
                    default -> System.out.println("Invalid option. Please enter a number from 0 to 6.");
                }
            } catch (SQLException e) {
                System.out.println("\n[DATABASE ERROR] " + e.getMessage());
            } catch (InvalidStatusException e) {
                System.out.println("\n[INVALID STATUS] " + e.getMessage());
            } catch (Exception e) {
                System.out.println("\n[ERROR] " + e.getMessage());
            }

            System.out.println();
        }
    }

    private static User authenticate() {
        while (true) {
            System.out.println("\n==================================================================");
            System.out.println("          CAREERTRACK - Job & Internship Tracker");
            System.out.println("==================================================================");
            System.out.println("1. Sign in");
            System.out.println("2. Create account");
            System.out.println("0. Exit");

            try {
                switch (prompt("Select an option")) {
                    case "1" -> {
                        String email = prompt("Email").trim().toLowerCase();
                        String password = prompt("Password");
                        User user = authService.authenticate(email, password);

                        if (user == null) {
                            System.out.println("Invalid email or password.");
                        } else {
                            System.out.println("\nWelcome, " + user.getFullName() + "!");
                            return user;
                        }
                    }
                    case "2" -> {
                        String fullName = prompt("Full name").trim();
                        String email = prompt("Email").trim().toLowerCase();
                        String password = prompt("Password");

                        try {
                            User user = authService.register(fullName, email, password);
                            System.out.println("Account created successfully.");
                            return user;
                        } catch (AuthService.RegistrationException e) {
                            System.out.println(registrationErrorMessage(e.getReason()));
                        }
                    }
                    case "0" -> {
                        return null;
                    }
                    default -> System.out.println("Invalid option.");
                }
            } catch (SQLException e) {
                System.out.println("[DATABASE ERROR] " + e.getMessage());
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
    }

    private static String registrationErrorMessage(AuthService.RegistrationException.Reason reason) {
        return switch (reason) {
            case INVALID_NAME -> "Please enter your full name.";
            case INVALID_EMAIL -> "Please enter a valid email address.";
            case WEAK_PASSWORD -> "Password must be at least 6 characters long.";
            case DUPLICATE_NAME -> "An account already exists with that name.";
            case DUPLICATE_EMAIL -> "An account already exists with that email.";
        };
    }

    private static void printMainMenu(User user) {
        System.out.println("\n---------------- CAREERTRACK DASHBOARD ----------------");
        System.out.println("User: " + user.getFullName() + " (" + user.getEmail() + ")");
        System.out.println("1. Companies");
        System.out.println("2. Application Management");
        System.out.println("3. Interview Tracking");
        System.out.println("4. Offer Management");
        System.out.println("5. Follow-ups");
        System.out.println("6. Reports and Analytics");
        System.out.println("0. Exit");
        System.out.println("-------------------------------------------------------");
    }

    private static void handleCompanies() throws SQLException {
        System.out.println("\n--- COMPANIES ---");
        System.out.println("1. View companies");
        System.out.println("2. Add company");
        System.out.println("3. Delete company");
        System.out.println("0. Back");

        switch (prompt("Choose")) {
            case "1" -> printList(companyDAO.getAllCompanies());
            case "2" -> {
                Company company = new Company(
                        prompt("Company name"),
                        prompt("Location"),
                        prompt("Role offered"),
                        prompt("Contact email"));
                companyDAO.addCompany(company);
                System.out.println("Company added successfully.");
            }
            case "3" -> {
                companyDAO.deleteCompany(readInt("Company ID"));
                System.out.println("Company deleted.");
            }
            default -> {
            }
        }
    }

    private static void handleApplications(User user) throws SQLException, InvalidStatusException {
        System.out.println("\n--- APPLICATION MANAGEMENT ---");
        System.out.println("1. View my applications");
        System.out.println("2. Add application");
        System.out.println("3. Update application status");
        System.out.println("4. Search my applications");
        System.out.println("5. Delete application");
        System.out.println("0. Back");

        switch (prompt("Choose")) {
            case "1" -> printList(applicationDAO.getAllApplicationsForUser(user.getUserId()));
            case "2" -> addApplication(user);
            case "3" -> {
                int applicationId = readInt("Application ID");
                String status = prompt("New status");
                applicationDAO.updateStatusForUser(applicationId, user.getUserId(), status);
                System.out.println("Status updated.");
            }
            case "4" -> {
                String keyword = prompt("Keyword").trim();
                List<Application> applications =
                        applicationDAO.searchAndFilterForUser(keyword, "", "", user.getUserId());
                printList(applications);
            }
            case "5" -> {
                applicationDAO.deleteApplicationForUser(readInt("Application ID"), user.getUserId());
                System.out.println("Application deleted.");
            }
            default -> {
            }
        }
    }

    private static void addApplication(User user) throws SQLException {
        int companyId = readInt("Company ID");
        String role = prompt("Role");
        String type = prompt("Type (Job or Internship)");
        LocalDate appliedDate = readDate("Applied date (YYYY-MM-DD)");
        String status = prompt("Status");
        String followUpText = prompt("Follow-up date (YYYY-MM-DD, or leave blank)");
        LocalDate followUpDate = followUpText.isBlank() ? null : LocalDate.parse(followUpText);

        Application application = "Internship".equalsIgnoreCase(type)
                ? new InternshipApplication(companyId, role, appliedDate, status, followUpDate)
                : new JobApplication(companyId, role, appliedDate, status, followUpDate);

        applicationDAO.addApplicationForUser(application, user.getUserId());
        System.out.println("Application added successfully.");
    }

    private static void handleInterviews(User user) throws SQLException {
        System.out.println("\n--- INTERVIEW TRACKING ---");
        System.out.println("1. View my interviews");
        System.out.println("2. Schedule interview");
        System.out.println("3. Update interview result");
        System.out.println("4. Delete interview");
        System.out.println("0. Back");

        switch (prompt("Choose")) {
            case "1" -> printList(interviewDAO.getAllInterviewsForUser(user.getUserId()));
            case "2" -> {
                Interview interview = new Interview(
                        readInt("Application ID"),
                        readDate("Interview date (YYYY-MM-DD)"),
                        prompt("Round"),
                        prompt("Result"));
                interviewDAO.addInterviewForUser(interview, user.getUserId());
                System.out.println("Interview scheduled.");
            }
            case "3" -> {
                interviewDAO.updateResultForUser(
                        readInt("Interview ID"), user.getUserId(), prompt("New result"));
                System.out.println("Interview result updated.");
            }
            case "4" -> {
                interviewDAO.deleteInterviewForUser(readInt("Interview ID"), user.getUserId());
                System.out.println("Interview deleted.");
            }
            default -> {
            }
        }
    }

    private static void handleOffers(User user) throws SQLException {
        System.out.println("\n--- OFFER MANAGEMENT ---");
        System.out.println("1. View my offers");
        System.out.println("2. Record offer");
        System.out.println("3. Update offer status");
        System.out.println("4. Delete offer");
        System.out.println("0. Back");

        switch (prompt("Choose")) {
            case "1" -> printList(offerDAO.getAllOffersForUser(user.getUserId()));
            case "2" -> {
                Offer offer = new Offer(
                        readInt("Application ID"),
                        readDouble("Package (LPA)"),
                        readDate("Deadline (YYYY-MM-DD)"),
                        prompt("Offer status"));
                offerDAO.addOfferForUser(offer, user.getUserId());
                System.out.println("Offer recorded.");
            }
            case "3" -> {
                offerDAO.updateOfferStatusForUser(
                        readInt("Offer ID"), user.getUserId(), prompt("New status"));
                System.out.println("Offer status updated.");
            }
            case "4" -> {
                offerDAO.deleteOfferForUser(readInt("Offer ID"), user.getUserId());
                System.out.println("Offer deleted.");
            }
            default -> {
            }
        }
    }

    private static void handleFollowUps(User user) throws SQLException {
        List<Application> applications =
                applicationDAO.getAllApplicationsForUser(user.getUserId());
        System.out.println("\n--- UPCOMING FOLLOW-UPS (NEXT 7 DAYS) ---");
        printList(ReportUtil.getUpcomingFollowUps(applications, 7));
        System.out.println("\n--- OVERDUE FOLLOW-UPS ---");
        printList(ReportUtil.getOverdueFollowUps(applications));

        if (!prompt("Set a follow-up date? (y/n)").equalsIgnoreCase("y")) {
            return;
        }

        applicationDAO.setFollowUpDateForUser(
                readInt("Application ID"),
                user.getUserId(),
                readDate("Follow-up date (YYYY-MM-DD)"));
        System.out.println("Follow-up date updated.");
    }

    private static void handleReports(User user) throws SQLException {
        List<Application> applications =
                applicationDAO.getAllApplicationsForUser(user.getUserId());
        List<Interview> interviews =
                interviewDAO.getAllInterviewsForUser(user.getUserId());
        List<Offer> offers =
                offerDAO.getAllOffersForUser(user.getUserId());

        System.out.println("\n==================================================================");
        System.out.println("                    CAREERTRACK REPORT");
        System.out.println("==================================================================");
        System.out.println("Total applications : " + applications.size());
        System.out.println("Total interviews   : " + interviews.size());
        System.out.println("Total offers       : " + offers.size());
        System.out.println("Rejected applications: " +
                applications.stream().filter(a -> "Rejected".equalsIgnoreCase(a.getStatus())).count());

        Map<String, Long> statusCounts =
                ReportUtil.getApplicationsGroupedByStatus(applications);
        System.out.println("\nApplications by status:");
        statusCounts.forEach((status, count) ->
                System.out.println(" * " + status + ": " + count));

        Map<String, Long> companyCounts =
                ReportUtil.getApplicationsPerCompany(applications);
        System.out.println("\nApplications by company:");
        companyCounts.forEach((company, count) ->
                System.out.println(" * " + company + ": " + count));

        System.out.printf("\nOffer rate: %.1f%% (%d offers / %d applications)%n",
                ReportUtil.calculateOfferRate(offers.size(), applications.size()),
                offers.size(),
                applications.size());

        ReportUtil.getHighestPackage(offers).ifPresentOrElse(
                packageLpa -> System.out.printf("Highest package: %.2f LPA%n", packageLpa),
                () -> System.out.println("Highest package: No offers recorded"));
    }

    private static String prompt(String label) {
        System.out.print(label + ": ");
        return scanner.nextLine().trim();
    }

    private static int readInt(String label) {
        return Integer.parseInt(prompt(label));
    }

    private static double readDouble(String label) {
        return Double.parseDouble(prompt(label));
    }

    private static LocalDate readDate(String label) {
        return LocalDate.parse(prompt(label));
    }

    private static void printList(List<?> items) {
        if (items.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (Object item : items) {
            System.out.println("  " + item);
        }
    }
}

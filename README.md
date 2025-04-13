# CivicFix - Issue Resolver

## Introduction
CivicFix is a mobile application aimed at improving civic engagement in Bangladesh by providing a digital platform for citizens to report local public issues. These issues—ranging from broken roads and water drainage problems to damaged footpaths and faulty streetlights—often go unresolved due to a lack of direct communication between citizens and local authorities.

CivicFix bridges this gap by allowing citizens to report issues within their district and ensuring that only verified government employees are able to view, manage, and update issues relevant to their area. The app emphasizes transparency, community moderation, and user accountability, all while being lightweight enough to be built using Android Studio.

## Key Features & Descriptions

### 🔑 1. User Registration & Login (Email & Password)
- Citizens and government employees can register using their email and password.
- During registration, users must select their district from a dropdown (no GPS needed).
- A role dropdown lets the user choose between "Citizen" or "Government Employee".
  - **Citizens** complete a simple registration form.
  - **Government employees** must also submit additional verification details (name, office name, designation, ID image).
- Access for government employees is restricted until approved by an admin.

### 📍 2. Issue Reporting (For Citizens)
- Citizens can report an issue in their district by providing:
  - Title
  - Description
  - Category (broken road, drainage, footpath, streetlight)
  - Optional Photo Upload
  - Manual area input (e.g., “Near XYZ School”)
- Each issue is tagged to the district selected during registration.
- Reports are stored in Firestore and associated with the reporting user.

### 🧰 3. Issue Management (For Government Employees)
- Once approved, government employees can:
  - View issues only in their assigned district.
  - Filter and manage issues by status or type.
  - Update issue status to:
    - Pending
    - In Progress
    - Resolved

### 📋 4. Home Feed & Issue Browsing
- The home page shows a search bar and a scrollable list of issues for the user’s district (like YouTube layout).
- Each issue in the feed displays:
  - Reporter’s icon
  - Title and short description
  - Upvote button
  - Issue status (Pending/In Progress/Resolved)
  - Submission time

### 📄 5. Issue Details Page
- Tapping on an issue opens a full details page with:
  - Complete description and photo (if any)
  - Area information
  - Submitted by (with profile link)
  - Current status
  - Taken by (for resolved/in progress)
- Users can:
  - Report the issue if it seems fake
  - Report the government employee who resolved it if unsatisfactory
  - Edit the issue (only if no employee has taken it yet)

### 🗃️ 6. Profile System
- Every user has a personal profile showing:
  - Profile image, name, and registration details
  - **Citizen profile**: Number of issues submitted
  - **Gov. employee profile**: Number of issues taken, solved, and reported after being marked as done

### 🔑 7. Navigation Bar Options
The app will have a bottom navigation bar with four main options:
- **Home Page**: Displays all issues within the user’s district. Issues are shown in a feed layout, with the ability to upvote, see status updates, and tap for more details.
- **User Search**: Users can search for other citizens or government employees by name or district. Each profile shows basic information and their activity (submitted issues for citizens, taken/resolved issues for government employees).
- **My Profile**: Displays the user’s personal information, such as their profile image, registration details, and the number of issues they’ve submitted or resolved (for government employees). Allows users to edit their profile details.
- **My Issue Info**: Citizens can see a list of all the issues they’ve submitted, along with their current statuses (pending, in progress, resolved). Government employees can see a list of issues they’ve taken, resolved, or resolved and reported.

### 🧑‍🤝‍🧑 8. User & Employee Lookup
- A dedicated “User” tab lets anyone search for users or government employees by name or district.
- Clicking a name leads to their public profile.

### 🗂️ 9. My Issues Section
- Citizens can view a list of all issues they’ve submitted with current statuses.
- Government employees see:
  - Pending issues they’ve taken
  - Resolved issues
  - Resolved & reported issues

### ⚠️ 10. Community Moderation & Flagging
- Users can flag any suspicious report.
- Flagged reports go into an admin review system (could be handled manually for now).
- Users or employees with multiple confirmed reports may be restricted from using the app.

## ✅ Technologies Used
- Android Studio (Java)
- Firebase Authentication (email/password)
- Firebase Firestore (issue & user data)
- Firebase Storage (for photo uploads)

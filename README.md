# 🐦 Okara - Social Media Platform

A modern Twitter-like social media platform built with Spring Boot and React. Features real-time engagement algorithms, interactive UI components, and comprehensive social media functionality.

![GitHub license](https://img.shields.io/github/license/yourusername/okara)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen)
![React](https://img.shields.io/badge/React-18.3.1-blue)
![TypeScript](https://img.shields.io/badge/TypeScript-5.6.3-blue)

## ✨ Features

### 🎯 Core Social Media Features
- **User Authentication** - Firebase Auth integration with secure JWT tokens
- **Tweet Posting** - Create, edit, and delete tweets with 280-character limit
- **Interactive Engagement** - Like, dislike, retweet, and comment on posts
- **Tweet Detail View** - Click on tweets to view replies and detailed interactions
- **Real-time Updates** - Live engagement counts and instant UI feedback

### 🧠 Advanced Features
- **Engagement Algorithm** - Sophisticated scoring system with diminishing returns
- **Anti-spam Protection** - Smart comment scoring prevents spam abuse
- **Dark/Light Mode** - Beautiful theme switching with Chakra UI
- **Mobile-Friendly** - Responsive design optimized for all devices
- **Performance Optimized** - Efficient database queries and caching

### 🛡️ Security & Performance
- **Firebase Authentication** - Enterprise-grade user authentication
- **MongoDB Integration** - Scalable NoSQL database with indexing
- **Optimized Queries** - Batch operations and efficient data fetching
- **Type Safety** - Full TypeScript coverage for runtime safety

## 🏗️ Architecture

```
okara/
├── 🔧 Backend (Spring Boot)
│   ├── src/main/java/io/shrouded/okara/
│   │   ├── controller/          # REST API endpoints
│   │   ├── service/            # Business logic & algorithms
│   │   ├── model/              # Data models (User, Feed)
│   │   ├── repository/         # MongoDB repositories
│   │   ├── security/           # Firebase auth & security
│   │   └── config/             # Configuration classes
│   └── src/main/resources/
│       ├── application.yml     # App configuration
│       └── static/             # Built React app
│
├── 🎨 Frontend (React + TypeScript)
│   ├── src/
│   │   ├── components/         # Reusable UI components
│   │   ├── pages/              # Main application pages
│   │   ├── hooks/              # Custom React hooks
│   │   ├── services/           # API service layer
│   │   ├── stores/             # State management (Zustand)
│   │   └── types/              # TypeScript definitions
│   └── public/                 # Static assets
│
└── 🐳 Infrastructure
    ├── docker-compose.yml      # MongoDB & Redis setup
    └── gradle/                 # Build configuration
```

## 🚀 Quick Start

### Prerequisites
- **Java 21 LTS** or higher
- **Node.js 20.x LTS** or higher  
- **MongoDB** (via Docker or local installation)
- **Firebase Project** (for authentication)

### 1. Clone & Setup
```bash
git clone https://github.com/yourusername/okara.git
cd okara
```

### 2. Database Setup
```bash
# Start MongoDB with Docker Compose
docker-compose up -d mongodb

# Or install MongoDB locally
# See: https://docs.mongodb.com/manual/installation/
```

### 3. Firebase Configuration
1. Create a Firebase project at https://console.firebase.google.com
2. Enable Authentication with Google provider
3. Download service account key to `src/main/resources/firebase-adminsdk.json`
4. Add Firebase config to frontend (see Frontend Setup)

### 4. Backend Setup
```bash
# Build and run Spring Boot application
./gradlew bootRun

# Backend will start on http://localhost:8080
```

### 5. Frontend Setup
```bash
cd frontend

# Install dependencies
npm install

# Create .env.local file with Firebase config
cat > .env.local << EOF
REACT_APP_FIREBASE_API_KEY=your_api_key
REACT_APP_FIREBASE_AUTH_DOMAIN=your_project.firebaseapp.com
REACT_APP_FIREBASE_PROJECT_ID=your_project_id
REACT_APP_API_BASE_URL=http://localhost:8080/api
EOF

# Start development server
npm start

# Frontend will start on http://localhost:3000
```

### 6. Visit Application
Open http://localhost:3000 and sign in with Google to start using Okara!

## 📱 Usage

### Creating Posts
1. Sign in with your Google account
2. Use the composer at the top of the home feed
3. Write your tweet (up to 280 characters)
4. Click "Tweet" to publish

### Engaging with Content
- **❤️ Like/Unlike** - Click the heart icon to like posts
- **🔄 Retweet** - Share posts with your followers  
- **💬 Comment** - Click on a tweet to view detail page and reply
- **🗑️ Delete** - Remove your own posts and comments

### Viewing Conversations
- Click on any tweet content to view the detailed conversation
- See all replies and engage with individual comments
- Like and delete replies just like main posts

## 🔧 API Endpoints

### Authentication
- `POST /api/auth/login` - Login with Firebase token
- `GET /api/auth/me` - Get current user profile

### Feed Management  
- `GET /api/feed/main` - Get main feed with engagement scoring
- `POST /api/feed/post` - Create new post
- `GET /api/feed/{postId}` - Get specific post details
- `DELETE /api/feed/{postId}` - Delete post (author only)

### Engagement
- `POST /api/feed/{postId}/like` - Like/unlike post
- `POST /api/feed/{postId}/retweet` - Retweet/unretweet post  
- `POST /api/feed/{postId}/comment` - Add comment to post
- `GET /api/feed/{postId}/comments` - Get post comments

## 🎯 Engagement Algorithm

Okara uses a sophisticated engagement scoring system:

```java
// Engagement Score Calculation
score = (likes × 1.0) + (retweets × 1.5) + commentScore + timeDecay

// Comment scoring with diminishing returns prevents spam
commentScore = distinctCommenters × 2.0 + Math.log(1 + totalComments) × 0.5

// Time decay keeps content fresh
timeDecay = Math.max(0, 24 - hoursOld) × 0.1
```

**Features:**
- Retweets weighted higher than likes (1.5x vs 1.0x)
- Distinct commenters prevent spam abuse
- Logarithmic comment scaling for diminishing returns  
- Time decay promotes fresh content
- Real-time score updates on all interactions

## 🛠️ Development

### Project Structure
- **Backend**: Spring Boot 3.3.5 with Java 21
- **Frontend**: React 18.3.1 with TypeScript 5.6.3
- **Database**: MongoDB with optimized indexing
- **Authentication**: Firebase Auth with JWT tokens
- **UI Framework**: Chakra UI v3 with dark/light themes
- **State Management**: Zustand for client state
- **Build Tool**: Gradle 8.10.2 with Node plugin

### Build Commands
```bash
# Full production build
./gradlew build

# Run tests
./gradlew test

# Start development servers
./gradlew bootRun              # Backend
cd frontend && npm start       # Frontend

# Build frontend only
cd frontend && npm run build
```

### Code Quality
- **ESLint** - JavaScript/TypeScript linting
- **TypeScript** - Strict type checking enabled
- **Spring Boot Actuator** - Health monitoring and metrics
- **Lombok** - Reduced boilerplate code
- **SLF4J** - Structured logging

## 🔒 Security

- **Firebase Authentication** - Industry-standard OAuth2/JWT
- **CORS Configuration** - Properly configured for development/production
- **Input Validation** - Request validation and sanitization
- **Authorization** - User-specific data access controls
- **SQL Injection Prevention** - MongoDB ODM with parameterized queries

## 🚀 Deployment

### Docker Deployment
```bash
# Build the application
./gradlew build

# Create Docker image
docker build -t okara:latest .

# Run with Docker Compose
docker-compose up -d
```

### Manual Deployment
```bash
# Build production JAR
./gradlew build

# Run standalone
java -jar build/libs/okara-0.0.1-SNAPSHOT.jar
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines
- Follow existing code style and conventions
- Add tests for new features
- Update documentation for API changes
- Ensure all tests pass before submitting PR

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Spring Boot** - Powerful Java framework
- **React & TypeScript** - Modern frontend development
- **Chakra UI** - Beautiful component library
- **Firebase** - Authentication and hosting platform
- **MongoDB** - Flexible document database

## 📞 Support

- 🐛 **Bug Reports**: Open an issue on GitHub
- 💡 **Feature Requests**: Open an issue with the enhancement label
- 📚 **Documentation**: Check the wiki for detailed guides
- 💬 **Community**: Join our discussions in GitHub Discussions

---

**Made with ❤️ by the Okara Team**
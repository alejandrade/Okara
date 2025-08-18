// MongoDB initialization script
db = db.getSiblingDB('okara');

// Create application user
db.createUser({
  user: 'okara_user',
  pwd: 'okara_password',
  roles: [
    {
      role: 'readWrite',
      db: 'okara'
    }
  ]
});

// Create initial collections
db.createCollection('users');
db.createCollection('sessions');
db.createCollection('mongockChangeLog');

print('Database initialized successfully');
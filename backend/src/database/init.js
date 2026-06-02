const fs = require('fs');
const path = require('path');
const dbService = require('../services/dbService');

async function initializeDatabase() {
  console.log('--------------------------------------------------');
  console.log('⚙️ Starting Safari Sphere Database Initialization...');
  console.log('--------------------------------------------------');

  if (dbService.isMock) {
    console.error('❌ Error: Under Mock Database Mode. Please ensure DATABASE_URL is set in your environment!');
    process.exit(1);
  }

  try {
    const schemaPath = path.join(__dirname, 'schema.sql');
    let schemaSql = fs.readFileSync(schemaPath, 'utf8');

    // Clean syntax or splits on block levels
    // Remove comments and replace standard UUID extension with a fallback if not superuser
    console.log('📖 Reading schema.sql definitions...');
    
    // We execute standard SQL. To handle multiple statements, we can execute the whole script, or execute query by query.
    // pg Pool query() can execute multiple queries if separated by semicolons in a single string!
    console.log('💾 Running schema queries on PostgreSQL...');
    await dbService.query(schemaSql);
    
    console.log('✅ Success: Safari Sphere tables and performance indexes initialized successfully!');
    console.log('--------------------------------------------------');
    process.exit(0);
  } catch (err) {
    console.error('❌ Database Initialization Failed!');
    console.error(err.message || err);
    console.log('--------------------------------------------------');
    process.exit(1);
  }
}

initializeDatabase();

FROM node:20-alpine

# Create app directory inside container
WORKDIR /usr/src/app

# Install dependencies using local backend package json files
COPY package*.json ./
RUN npm install --omit=dev

# Copy backend files
COPY . .

# Set production port and expose it
ENV PORT=8080
EXPOSE 8080

# Start server
CMD [ "npm", "start" ]

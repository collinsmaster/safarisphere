FROM node:20-alpine

# Create app directory
WORKDIR /usr/src/app

# Install app dependencies
COPY backend/package*.json ./
RUN npm install --omit=dev

# Bundle app source
COPY backend/ .

# Set dynamic port via environment and expose it
ENV PORT=8080
EXPOSE 8080

# Run production server
CMD [ "npm", "start" ]

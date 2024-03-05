FROM node:lts-alpine3.18 as base
EXPOSE 3000

WORKDIR /srv/frontend

#development build
FROM base AS build
COPY . /srv/frontend

RUN npm install && \
	npm run build

CMD npm start


#production build
FROM base AS prod

COPY --from=build /srv/frontend/build /srv/frontend
COPY serve.json /srv/frontend/

RUN npm install -g serve
CMD serve /srv/frontend
